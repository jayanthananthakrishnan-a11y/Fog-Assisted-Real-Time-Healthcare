package priority_aware;

import common.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;
import org.fog.utils.Config;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Architecture D: Priority-Aware Fog Processing.
 * 
 * Rules:
 * 1. CRITICAL: Zero aggregation wait. Dual immediate dispatch:
 *    - Local emergency alert to Bedside Actuator (~3 ms).
 *    - Urgent dispatch to Cloud Analytics for medical record archiving.
 * 2. ABNORMAL: Prompt expedited individual forwarding without aggregation delay.
 * 3. NORMAL: Evaluated through delta redundancy filter; non-redundant readings
 *    are batched in sliding window W (default W = 5).
 */
public class HealthcarePriorityFog {

    private static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    private static List<Sensor> sensors = new ArrayList<Sensor>();
    private static List<Actuator> actuators = new ArrayList<Actuator>();

    public static void main(String[] args) {
        SimulationConfig.parseArgs(args);

        MetricsTracker.reset();
        MetricsTracker.getInstance().setArchitectureName("Architecture D (Priority-Aware Fog Processing)");

        System.out.println("========================================================================");
        System.out.println(" Starting Architecture D: Priority-Aware Fog Simulation                 ");
        System.out.printf(" Patients: %d | Sensors: %d | Window Size W: %d | Duration: %.0f ms\n",
                SimulationConfig.numPatients,
                SimulationConfig.numPatients * SimulationConfig.SENSORS_PER_PATIENT,
                SimulationConfig.aggregationWindowSize,
                SimulationConfig.SIMULATION_DURATION);
        System.out.println("========================================================================");

        try {
            Log.disable();
            int numUser = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;

            CloudSim.init(numUser, calendar, traceFlag);

            String appId = SimulationConfig.APP_ID;
            FogBroker broker = new FogBroker("broker");

            // 1. Build application DAG
            Application application = Application.createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

            application.addAppModule(SimulationConfig.MODULE_FOG_PRIORITY_PROCESSOR, SimulationConfig.MODULE_FOG_RAM);
            application.addAppModule(SimulationConfig.MODULE_CLOUD_ANALYTICS, SimulationConfig.MODULE_CLOUD_ANALYTICS_RAM);

            // Sensor -> fog_priority_processor
            for (String vitalSign : VitalSignType.ALL_TYPES) {
                application.addAppEdge(
                        vitalSign,
                        SimulationConfig.MODULE_FOG_PRIORITY_PROCESSOR,
                        SimulationConfig.FOG_PRIORITY_CPU_LENGTH,
                        SimulationConfig.RAW_TUPLE_NW_SIZE,
                        vitalSign,
                        Tuple.UP,
                        AppEdge.SENSOR
                );
            }

            // fog_priority_processor -> cloud_analytics edges
            application.addAppEdge(
                    SimulationConfig.MODULE_FOG_PRIORITY_PROCESSOR,
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    SimulationConfig.AGGREGATED_CLOUD_CPU_LENGTH,
                    SimulationConfig.AGGREGATED_TUPLE_NW_SIZE,
                    "AGGREGATED_VITALS",
                    Tuple.UP,
                    AppEdge.MODULE
            );
            application.addAppEdge(
                    SimulationConfig.MODULE_FOG_PRIORITY_PROCESSOR,
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    SimulationConfig.CLOUD_ANALYTICS_CPU_LENGTH,
                    SimulationConfig.RAW_TUPLE_NW_SIZE,
                    "ABNORMAL_EXPEDITED_VITALS",
                    Tuple.UP,
                    AppEdge.MODULE
            );
            application.addAppEdge(
                    SimulationConfig.MODULE_FOG_PRIORITY_PROCESSOR,
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    SimulationConfig.CLOUD_ANALYTICS_CPU_LENGTH,
                    SimulationConfig.RAW_TUPLE_NW_SIZE,
                    "CRITICAL_URGENT_VITALS",
                    Tuple.UP,
                    AppEdge.MODULE
            );

            // Downlink edges to actuator
            application.addAppEdge(
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    SimulationConfig.ACTUATOR_DISPLAY,
                    SimulationConfig.ANALYTICS_RESULT_CPU_LENGTH,
                    SimulationConfig.ANALYTICS_RESULT_NW_SIZE,
                    "AGGREGATED_RESULT",
                    Tuple.DOWN,
                    AppEdge.ACTUATOR
            );
            application.addAppEdge(
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    SimulationConfig.ACTUATOR_DISPLAY,
                    SimulationConfig.ANALYTICS_RESULT_CPU_LENGTH,
                    SimulationConfig.ANALYTICS_RESULT_NW_SIZE,
                    "EXPEDITED_RESULT",
                    Tuple.DOWN,
                    AppEdge.ACTUATOR
            );
            application.addAppEdge(
                    SimulationConfig.MODULE_FOG_PRIORITY_PROCESSOR,
                    SimulationConfig.ACTUATOR_DISPLAY,
                    SimulationConfig.ANALYTICS_RESULT_CPU_LENGTH,
                    SimulationConfig.ANALYTICS_RESULT_NW_SIZE,
                    "CRITICAL_LOCAL_ALERT",
                    Tuple.DOWN,
                    AppEdge.ACTUATOR
            );

            // Cloud selectivity mappings
            application.addTupleMapping(
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    "AGGREGATED_VITALS",
                    "AGGREGATED_RESULT",
                    new FractionalSelectivity(1.0)
            );
            application.addTupleMapping(
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    "ABNORMAL_EXPEDITED_VITALS",
                    "EXPEDITED_RESULT",
                    new FractionalSelectivity(1.0)
            );
            application.addTupleMapping(
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    "CRITICAL_URGENT_VITALS",
                    "EXPEDITED_RESULT",
                    new FractionalSelectivity(1.0)
            );

            // 2. Build physical hierarchy
            PriorityProcessorEngine engine = new PriorityProcessorEngine(SimulationConfig.aggregationWindowSize);
            VitalSignGenerator generator = new VitalSignGenerator(SimulationConfig.RANDOM_SEED);

            // Level 0: Cloud Datacenter
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

            // Level 1: Shared Hospital Fog Gateway hosting the PriorityProcessorEngine
            PriorityFogDevice hospitalFog = PriorityFogDevice.createPriorityDevice(
                    SimulationConfig.HOSPITAL_FOG_NAME,
                    SimulationConfig.HOSPITAL_FOG_MIPS,
                    SimulationConfig.HOSPITAL_FOG_RAM,
                    (long) SimulationConfig.HOSPITAL_FOG_UP_BW,
                    (long) SimulationConfig.HOSPITAL_FOG_DOWN_BW,
                    1,
                    SimulationConfig.HOSPITAL_FOG_RATE_PER_MIPS,
                    SimulationConfig.HOSPITAL_FOG_BUSY_POWER,
                    SimulationConfig.HOSPITAL_FOG_IDLE_POWER,
                    engine
            );
            hospitalFog.setBrokerId(broker.getId());
            hospitalFog.setParentId(cloud.getId());
            hospitalFog.setUplinkLatency(SimulationConfig.wanLatency); // Configurable WAN link
            hospitalFog.setUplinkBandwidth(SimulationConfig.wanBandwidth);
            hospitalFog.setDownlinkBandwidth(SimulationConfig.wanBandwidth);
            cloud.setUplinkBandwidth(SimulationConfig.wanBandwidth);
            cloud.setDownlinkBandwidth(SimulationConfig.wanBandwidth);
            fogDevices.add(hospitalFog);

            // Level 2: Patient Gateways, Sensors, and Actuators
            for (int i = 0; i < SimulationConfig.numPatients; i++) {
                String gatewayName = SimulationConfig.GATEWAY_PREFIX + i;
                HealthcareFogDevice patientGateway = HealthcareFogDevice.createDevice(
                        gatewayName,
                        SimulationConfig.GATEWAY_MIPS,
                        SimulationConfig.GATEWAY_RAM,
                        (long) SimulationConfig.GATEWAY_UP_BW,
                        (long) SimulationConfig.GATEWAY_DOWN_BW,
                        2,
                        SimulationConfig.GATEWAY_RATE_PER_MIPS,
                        SimulationConfig.GATEWAY_BUSY_POWER,
                        SimulationConfig.GATEWAY_IDLE_POWER
                );
                patientGateway.setParentId(hospitalFog.getId());
                patientGateway.setUplinkLatency(SimulationConfig.LAN_LATENCY); // 2 ms LAN
                fogDevices.add(patientGateway);

                // 4 sensors per patient
                for (String vitalSign : VitalSignType.ALL_TYPES) {
                    HealthcareSensor sensor = new HealthcareSensor(
                            "s-" + i + "-" + vitalSign,
                            vitalSign,
                            broker.getId(),
                            appId,
                            i,
                            patientGateway.getId(),
                            SimulationConfig.SENSOR_LATENCY,
                            new DeterministicDistribution(SimulationConfig.READING_INTERVAL),
                            generator,
                            SimulationConfig.READINGS_PER_SENSOR
                    );
                    sensor.setApp(application);
                    sensors.add(sensor);
                }

                // 1 actuator per patient
                HealthcareActuator display = new HealthcareActuator(
                        "actuator-" + i,
                        broker.getId(),
                        appId,
                        i,
                        patientGateway.getId(),
                        SimulationConfig.ACTUATOR_LATENCY,
                        SimulationConfig.ACTUATOR_DISPLAY
                );
                display.setApp(application);
                actuators.add(display);
            }

            // 3. Module Placement
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            moduleMapping.addModuleToDevice(SimulationConfig.MODULE_FOG_PRIORITY_PROCESSOR, SimulationConfig.HOSPITAL_FOG_NAME);
            moduleMapping.addModuleToDevice(SimulationConfig.MODULE_CLOUD_ANALYTICS, SimulationConfig.CLOUD_NAME);

            // 4. Controller
            HealthcareController controller = new HealthcareController(
                    "master-controller", fogDevices, sensors, actuators,
                    SimulationConfig.READINGS_CSV_PATH_D, SimulationConfig.SUMMARY_CSV_PATH_D
            );
            controller.submitApplication(application, 0, new ModulePlacementMapping(fogDevices, application, moduleMapping));

            Config.MAX_SIMULATION_TIME = SimulationConfig.STOP_SIMULATION_TIME;
            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MetricsTracker.getInstance().exportToCsv(SimulationConfig.READINGS_CSV_PATH_D, SimulationConfig.SUMMARY_CSV_PATH_D);
            }));

            CloudSim.startSimulation();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[HealthcarePriorityFog] Error: " + e.getMessage());
        }
    }
}
