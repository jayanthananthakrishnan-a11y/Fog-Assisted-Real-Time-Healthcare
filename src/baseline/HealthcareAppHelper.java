package baseline;

import common.SimulationConfig;
import common.VitalSignType;
import java.util.ArrayList;
import java.util.List;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Tuple;

/**
 * Builds the dataflow DAG Application for Architecture A (Cloud-Only Baseline).
 * Sensor streams connect directly to cloud analytics, and analytics results
 * are delivered back to client displays.
 */
public class HealthcareAppHelper {

    public static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        // 1. Add application modules (vertices)
        application.addAppModule(SimulationConfig.MODULE_CLOUD_ANALYTICS, SimulationConfig.MODULE_CLOUD_ANALYTICS_RAM);

        // 2. Add application edges
        // Sensor -> Cloud Analytics (Uplink)
        for (String vitalSign : VitalSignType.ALL_TYPES) {
            application.addAppEdge(
                    vitalSign,
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    SimulationConfig.CLOUD_ANALYTICS_CPU_LENGTH,
                    SimulationConfig.RAW_TUPLE_NW_SIZE,
                    vitalSign,
                    Tuple.UP,
                    AppEdge.SENSOR
            );
        }

        // Cloud Analytics -> Client Display Actuator (Downlink)
        application.addAppEdge(
                SimulationConfig.MODULE_CLOUD_ANALYTICS,
                SimulationConfig.ACTUATOR_DISPLAY,
                SimulationConfig.ANALYTICS_RESULT_CPU_LENGTH,
                SimulationConfig.ANALYTICS_RESULT_NW_SIZE,
                SimulationConfig.TUPLE_ANALYTICS_RESULT,
                Tuple.DOWN,
                AppEdge.ACTUATOR
        );

        // 3. Define tuple input-output relationships (Selectivity)
        // Every incoming raw reading processed at the cloud emits an analytics result
        for (String vitalSign : VitalSignType.ALL_TYPES) {
            application.addTupleMapping(
                    SimulationConfig.MODULE_CLOUD_ANALYTICS,
                    vitalSign,
                    SimulationConfig.TUPLE_ANALYTICS_RESULT,
                    new FractionalSelectivity(1.0)
            );
        }

        // 4. Define application monitoring loops for latency evaluation
        List<AppLoop> loops = new ArrayList<AppLoop>();
        for (final String vitalSign : VitalSignType.ALL_TYPES) {
            AppLoop loop = new AppLoop(new ArrayList<String>() {{
                add(vitalSign);
                add(SimulationConfig.MODULE_CLOUD_ANALYTICS);
                add(SimulationConfig.ACTUATOR_DISPLAY);
            }});
            loops.add(loop);
        }
        application.setLoops(loops);

        return application;
    }
}
