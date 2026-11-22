package baseline;

import common.HealthcareActuator;
import common.HealthcareController;
import common.HealthcareFogDevice;
import common.HealthcareSensor;
import common.MetricsTracker;
import common.SimulationConfig;
import common.VitalSignGenerator;
import common.VitalSignType;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;
import org.fog.utils.Config;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Architecture A: Healthcare Cloud-Only Baseline.
 * 
 * Dataflow:
 * Healthcare Sensors ---> Patient Gateway (Bridge) ---> Remote Cloud Datacenter ---> Cloud Analytics ---> Actuator
 * 
 * Every raw reading is transmitted directly to the Cloud without local fog filtering or aggregation.
 * This establishes our empirical baseline for evaluation against Architectures B, C, and D.
 */
public class HealthcareCloudOnly {

    private static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    private static List<Sensor> sensors = new ArrayList<Sensor>();
    private static List<Actuator> actuators = new ArrayList<Actuator>();

    public static void main(String[] args) {
        SimulationConfig.parseArgs(args);

        System.out.println("========================================================================");
        System.out.println(" Starting Architecture A: Cloud-Only Healthcare Baseline Simulation   ");
        System.out.printf(" Patients: %d | Sensors: %d | Total Duration: %.0f ms\n",
                SimulationConfig.numPatients,
                SimulationConfig.numPatients * SimulationConfig.SENSORS_PER_PATIENT,
                SimulationConfig.SIMULATION_DURATION);
        System.out.println("========================================================================");

        try {
            Log.disable();
            int numUser = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;

            // 1. Initialize discrete event simulation core
            CloudSim.init(numUser, calendar, traceFlag);

            String appId = SimulationConfig.APP_ID;
            FogBroker broker = new FogBroker("broker");

            // 2. Build application dataflow DAG
            Application application = HealthcareAppHelper.createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

            // 3. Build physical topology
            // Deterministic generator with fixed seed
            VitalSignGenerator generator = new VitalSignGenerator(SimulationConfig.RANDOM_SEED);

            // Cloud Datacenter (Level 0 Root)
            HealthcareFogDevice cloud = HealthcareFogDevice.createDevice(
                    SimulationConfig.CLOUD_NAME,
                    SimulationConfig.CLOUD_MIPS,
                    SimulationConfig.CLOUD_RAM,
                    (long) SimulationConfig.CLOUD_UP_BW,
                    (long) SimulationConfig.CLOUD_DOWN_BW,
                    0,
                    SimulationConfig.CLOUD_RATE_PER_MIPS,
                    SimulationConfig.CLOUD_BUSY_POWER,
                    SimulationConfig.CLOUD_IDLE_POWER
            );
            cloud.setParentId(-1);
            fogDevices.add(cloud);

            // Patient Gateways, Sensors, and Actuators
            for (int i = 0; i < SimulationConfig.numPatients; i++) {
                String gatewayName = SimulationConfig.GATEWAY_PREFIX + i;
                HealthcareFogDevice gateway = HealthcareFogDevice.createDevice(
                        gatewayName,
                        SimulationConfig.GATEWAY_MIPS,
                        SimulationConfig.GATEWAY_RAM,
                        (long) SimulationConfig.GATEWAY_UP_BW,
                        (long) SimulationConfig.GATEWAY_DOWN_BW,
                        1,
                        SimulationConfig.GATEWAY_RATE_PER_MIPS,
                        SimulationConfig.GATEWAY_BUSY_POWER,
                        SimulationConfig.GATEWAY_IDLE_POWER
                );
                gateway.setParentId(cloud.getId());
                gateway.setUplinkLatency(SimulationConfig.wanLatency); // Configurable WAN latency
                gateway.setUplinkBandwidth(SimulationConfig.wanBandwidth);
                fogDevices.add(gateway);

                // Add 4 vital sign sensors per patient
                for (String vitalSign : VitalSignType.ALL_TYPES) {
                    HealthcareSensor sensor = new HealthcareSensor(
                            "s-" + i + "-" + vitalSign,
                            vitalSign,
                            broker.getId(),
                            appId,
                            i,
                            gateway.getId(),
                            SimulationConfig.SENSOR_LATENCY,
                            new DeterministicDistribution(SimulationConfig.READING_INTERVAL),
                            generator,
                            SimulationConfig.READINGS_PER_SENSOR
                    );
                    sensor.setApp(application);
                    sensors.add(sensor);
                }

                // Add 1 client display actuator per patient
                HealthcareActuator display = new HealthcareActuator(
                        "actuator-" + i,
                        broker.getId(),
                        appId,
                        i,
                        gateway.getId(),
                        SimulationConfig.ACTUATOR_LATENCY,
                        SimulationConfig.ACTUATOR_DISPLAY
                );
                display.setApp(application);
                actuators.add(display);
            }

            // 4. Module Placement Mapping: All analytics placed strictly on Cloud Datacenter
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            moduleMapping.addModuleToDevice(SimulationConfig.MODULE_CLOUD_ANALYTICS, SimulationConfig.CLOUD_NAME);

            // 5. Submit application to custom HealthcareController
            HealthcareController controller = new HealthcareController("master-controller", fogDevices, sensors, actuators);
            controller.submitApplication(application, 0, new ModulePlacementMapping(fogDevices, application, moduleMapping));

            // Set simulation stop time (with buffer to drain all in-flight packets)
            Config.MAX_SIMULATION_TIME = SimulationConfig.STOP_SIMULATION_TIME;

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            // Register safety shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MetricsTracker.getInstance().exportToCsv();
            }));

            // 6. Start simulation
            CloudSim.startSimulation();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[HealthcareCloudOnly] Fatal simulation error: " + e.getMessage());
        }
    }
}
