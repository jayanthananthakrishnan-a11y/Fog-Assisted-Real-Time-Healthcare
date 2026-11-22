package common;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.entities.Actuator;
import org.fog.entities.Tuple;
import org.fog.utils.FogEvents;

/**
 * Custom healthcare actuator representing clinical bedside monitor or alert display.
 * Intercepts tuple arrival to record exact completion timestamps in MetricsTracker.
 */
public class HealthcareActuator extends Actuator {

    private final int patientId;

    public HealthcareActuator(String name, int userId, String appId, int patientId,
                              int gatewayDeviceId, double latency, String actuatorType) {
        super(name, userId, appId, actuatorType);
        this.patientId = patientId;
        setGatewayDeviceId(gatewayDeviceId);
        setLatency(latency);
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == FogEvents.TUPLE_ARRIVAL) {
            Tuple tuple = (Tuple) ev.getData();
            // Only record delivery for individual raw readings (Architecture A and B)
            // Architecture C & D aggregated results and emergency alerts are recorded by their respective fog devices
            if (tuple.getTupleType().equals(SimulationConfig.TUPLE_ANALYTICS_RESULT)) {
                MetricsTracker.getInstance().recordDelivery(tuple.getActualTupleId(), CloudSim.clock());
            }
        }
        super.processEvent(ev);
    }

    public int getPatientId() {
        return patientId;
    }
}
