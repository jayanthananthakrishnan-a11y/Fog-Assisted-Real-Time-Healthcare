package common;

import java.util.ArrayList;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.Distribution;

/**
 * Custom healthcare sensor that generates synthetic vital signs
 * and injects them as standard iFogSim Tuples into the network.
 */
public class HealthcareSensor extends Sensor {

    private final int patientId;
    private final VitalSignGenerator vitalSignGenerator;
    private int readingsCount = 0;
    private final int maxReadings;

    public HealthcareSensor(String name, String tupleType, int userId, String appId,
                            int patientId, int gatewayDeviceId, double latency,
                            Distribution transmitDistribution, VitalSignGenerator generator,
                            int maxReadings) {
        super(name, tupleType, userId, appId, transmitDistribution);
        this.patientId = patientId;
        this.vitalSignGenerator = generator;
        this.maxReadings = maxReadings;
        setGatewayDeviceId(gatewayDeviceId);
        setLatency(latency);
    }

    @Override
    public void transmit() {
        if (readingsCount >= maxReadings) {
            return;
        }

        AppEdge _edge = null;
        for (AppEdge edge : getApp().getEdges()) {
            if (edge.getSource().equals(getTupleType())) {
                _edge = edge;
                break;
            }
        }

        if (_edge == null) {
            System.err.println("[HealthcareSensor] No outgoing edge found for tupleType: " + getTupleType());
            return;
        }

        long cpuLength = (long) _edge.getTupleCpuLength();
        long nwLength = (long) _edge.getTupleNwLength();

        // Register loop timing and acquire consistent readingId
        int readingId = updateTimings(getSensorName(), _edge.getDestination());
        if (readingId == -1) {
            readingId = TimeKeeper.getInstance().getUniqueId();
            TimeKeeper.getInstance().getEmitTimes().put(readingId, CloudSim.clock());
        }

        // Generate synthetic physiological reading
        ReadingRecord record = vitalSignGenerator.generateReading(
                readingId, patientId, getGatewayDeviceId(), getTupleType(), CloudSim.clock());
        MetricsTracker.getInstance().recordGeneration(record);

        // Instantiate standard iFogSim Tuple
        Tuple tuple = new Tuple(getAppId(), FogUtils.generateTupleId(), Tuple.UP, cpuLength, 1, nwLength,
                getOutputSize(), new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
        tuple.setUserId(getUserId());
        tuple.setTupleType(getTupleType());
        tuple.setDestModuleName(_edge.getDestination());
        tuple.setSrcModuleName(getSensorName());
        tuple.setDestinationDeviceId(getGatewayDeviceId());
        tuple.setSourceDeviceId(getGatewayDeviceId());
        tuple.setActualTupleId(readingId);

        // Send tuple to local patient gateway
        send(getGatewayDeviceId(), getLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
        readingsCount++;
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case FogEvents.TUPLE_ACK:
                break;
            case FogEvents.EMIT_TUPLE:
                if (readingsCount < maxReadings) {
                    transmit();
                    send(getId(), getTransmitDistribution().getNextValue(), FogEvents.EMIT_TUPLE);
                }
                break;
            default:
                super.processEvent(ev);
                break;
        }
    }

    public int getPatientId() {
        return patientId;
    }

    public int getReadingsCount() {
        return readingsCount;
    }
}
