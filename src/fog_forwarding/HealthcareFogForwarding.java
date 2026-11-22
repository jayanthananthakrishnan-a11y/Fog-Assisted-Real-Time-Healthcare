package fog_forwarding;

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
 * Architecture B: Fog Forwarding.
 * 
 * Topology:
 * Sensors (2 ms) -> Patient Gateway (2 ms) -> Hospital Fog Gateway (hosts fog_forwarder) 
 * -> WAN (100 ms) -> Cloud Datacenter (hosts cloud_analytics) -> WAN (100 ms) -> Actuator (1 ms).
 * 
 * Behavior:
 * The fog_forwarder module inspects every raw reading (200 MIPS) and forwards 100% of readings
 * to the cloud without filtering or aggregation.
 * 
 * Purpose:
 * Isolates the pure network hop and compute overhead of an intermediate fog node.
 */
public class HealthcareFogForwarding {

    private static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    private static List<Sensor> sensors = new ArrayList<Sensor>();
    private static List<Actuator> actuators = new ArrayList<Actuator>();

    public static void main(String[] args) {
        SimulationConfig.parseArgs(args);

        MetricsTracker.reset();
        MetricsTracker.getInstance().setArchitectureName("Architecture B (Fog Forwarding)");

        System.out.println("========================================================================");
        System.out.println(" Starting Architecture B: Fog Forwarding Simulation                     ");
        System.out.printf(" Patients: %d | Sensors: %d | Duration: %.0f ms\n",
                SimulationConfig.numPatients,
                SimulationConfig.numPatients * SimulationConfig.SENSORS_PER_PATIENT,
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

            // 1. Build application DAG with fog_forwarder and cloud_analytics
            Application application = Application.createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

            application.addAppModule(SimulationConfig.MODULE_FOG_FORWARDER, SimulationConfig.MODULE_FOG_RAM);
            application.addAppModule(SimulationConfig.MODULE_CLOUD_ANALYTICS, SimulationConfig.MODULE_CLOUD_ANALYTICS_RAM);

            // Sensor -> fog_forwarder (Uplink)
            for (String vitalSign : VitalSignType.ALL_TYPES) {
                application.addAppEdge(
                        vitalSign,
                        SimulationConfig.MODULE_FOG_FORWARDER,
                        SimulationConfig.FOG_FORWARD_CPU_LENGTH,
                        SimulationConfig.RAW_TUPLE_NW_SIZE,
                        vitalSign,
                        Tuple.UP,
                        AppEdge.SENSOR
                );
            }

            // fog_forwarder -> cloud_analytics (WAN Uplink)
            application.addAppEdge(
                    SimulationConfig.MODULE_FOG_FORWARDER,
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    SimulationConfig.CLOUD_ANALYTICS_CPU_LENGTH,
                    SimulationConfig.RAW_TUPLE_NW_SIZE,
                    "FORWARDED_RAW_DATA",
                    Tuple.UP,
                    AppEdge.MODULE
            );

            // cloud_analytics -> client_display (Downlink)
            application.addAppEdge(
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    SimulationConfig.ACTUATOR_DISPLAY,
                    SimulationConfig.ANALYTICS_RESULT_CPU_LENGTH,
                    SimulationConfig.ANALYTICS_RESULT_NW_SIZE,
                    SimulationConfig.TUPLE_ANALYTICS_RESULT,
                    Tuple.DOWN,
                    AppEdge.ACTUATOR
            );

            // Tuple mappings: 100% forwarding
            for (String vitalSign : VitalSignType.ALL_TYPES) {
                application.addTupleMapping(
                        SimulationConfig.MODULE_FOG_FORWARDER,
                        vitalSign,
                        "FORWARDED_RAW_DATA",
                        new FractionalSelectivity(1.0)
                );
            }
            application.addTupleMapping(
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    "FORWARDED_RAW_DATA",
                    SimulationConfig.TUPLE_ANALYTICS_RESULT,
                    new FractionalSelectivity(1.0)
            );

            // Application loops
            List<AppLoop> loops = new ArrayList<AppLoop>();
            for (final String vitalSign : VitalSignType.ALL_TYPES) {
                AppLoop loop = new AppLoop(new ArrayList<String>() {{
                    add(vitalSign);
                    add(SimulationConfig.MODULE_FOG_FORWARDER);
                    add(SimulationConfig.MODULE_CLOUD_ANALYTICS);
                    add(SimulationConfig.ACTUATOR_DISPLAY);
                }});
                loops.add(loop);
            }
            application.setLoops(loops);

            // 2. Build physical hierarchy
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

            // Level 1: Shared Hospital Fog Gateway
            HealthcareFogDevice hospitalFog = HealthcareFogDevice.createDevice(
                    SimulationConfig.HOSPITAL_FOG_NAME,
                    SimulationConfig.HOSPITAL_FOG_MIPS,
                    SimulationConfig.HOSPITAL_FOG_RAM,
                    (long) SimulationConfig.HOSPITAL_FOG_UP_BW,
                    (long) SimulationConfig.HOSPITAL_FOG_DOWN_BW,
                    1,
                    SimulationConfig.HOSPITAL_FOG_RATE_PER_MIPS,
                    SimulationConfig.HOSPITAL_FOG_BUSY_POWER,
                    SimulationConfig.HOSPITAL_FOG_IDLE_POWER
            );
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
                patientGateway.setUplinkLatency(SimulationConfig.LAN_LATENCY); // 2 ms local hospital LAN
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

            // 3. Module Placement: fog_forwarder -> hospital_fog_gateway, cloud_analytics -> cloud
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            moduleMapping.addModuleToDevice(SimulationConfig.MODULE_FOG_FORWARDER, SimulationConfig.HOSPITAL_FOG_NAME);
            moduleMapping.addModuleToDevice(SimulationConfig.MODULE_CLOUD_ANALYTICS, SimulationConfig.CLOUD_NAME);

            // 4. Submit application
            HealthcareController controller = new HealthcareController(
                    "master-controller", fogDevices, sensors, actuators,
                    SimulationConfig.READINGS_CSV_PATH_B, SimulationConfig.SUMMARY_CSV_PATH_B
            );
            controller.submitApplication(application, 0, new ModulePlacementMapping(fogDevices, application, moduleMapping));

            Config.MAX_SIMULATION_TIME = SimulationConfig.STOP_SIMULATION_TIME;
            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MetricsTracker.getInstance().exportToCsv(SimulationConfig.READINGS_CSV_PATH_B, SimulationConfig.SUMMARY_CSV_PATH_B);
            }));

            CloudSim.startSimulation();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[HealthcareFogForwarding] Error: " + e.getMessage());
        }
    }
}
