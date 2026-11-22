package fog_filtering;

import common.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Tuple;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogEvents;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;

/**
 * Hospital Fog Gateway device incorporating the FilterAggregatorEngine.
 * Drops redundant NORMAL readings and batches non-redundant readings into
 * sliding window tuples before transmitting over the WAN link.
 */
public class FilteringFogDevice extends HealthcareFogDevice {

    private final FilterAggregatorEngine engine;
    private int brokerId = 1;

    public FilteringFogDevice(String name, FogDeviceCharacteristics characteristics,
                              VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
                              double schedulingInterval, double uplinkBandwidth, double downlinkBandwidth,
                              double uplinkLatency, double ratePerMips, FilterAggregatorEngine engine) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval,
                uplinkBandwidth, downlinkBandwidth, uplinkLatency, ratePerMips);
        this.engine = engine;
    }

    public void setBrokerId(int brokerId) {
        this.brokerId = brokerId;
    }

    public int getBrokerId() {
        return brokerId;
    }

    private boolean buffersFlushed = false;

    @Override
    public void startEntity() {
        super.startEntity();
        send(getId(), SimulationConfig.SIMULATION_DURATION + 500.0, FogEvents.RESOURCE_MGMT);
    }

    @Override
    protected void processTupleArrival(SimEvent ev) {
        Tuple tuple = (Tuple) ev.getData();

        if (tuple.getUserId() > 0) {
            this.brokerId = tuple.getUserId();
        }

        // 1. Intercept incoming raw sensor readings at the Hospital Fog Gateway
        if (getName().equals(SimulationConfig.HOSPITAL_FOG_NAME) && tuple.getDirection() == Tuple.UP
                && !tuple.getTupleType().equals("AGGREGATED_VITALS")) {

            int readingId = tuple.getActualTupleId();
            ReadingRecord rec = MetricsTracker.getInstance().getReading(readingId);

            if (rec != null) {
                List<FilterAggregatorEngine.BatchInfo> readyBatches = new ArrayList<>();
                FilterAggregatorEngine.FilterDecision decision = engine.evaluateReading(rec, readyBatches);

                if (decision == FilterAggregatorEngine.FilterDecision.FILTERED_DROPPED) {
                    MetricsTracker.getInstance().recordFiltered(readingId);
                    // Do not transmit over WAN — dropped at fog
                    return;
                }

                if (decision == FilterAggregatorEngine.FilterDecision.BUFFERED_IN_WINDOW) {
                    // Retained in aggregation buffer until window W accumulates
                    return;
                }

                if (decision == FilterAggregatorEngine.FilterDecision.BATCH_READY) {
                    for (FilterAggregatorEngine.BatchInfo batch : readyBatches) {
                        dispatchAggregatedBatch(batch);
                    }
                    return;
                }
            }
        }

        // 2. Intercept aggregated results returning DOWN from the Cloud
        if ((tuple.getDirection() == Tuple.DOWN || tuple.getDirection() == Tuple.ACTUATOR)
                && tuple.getTupleType().equals("AGGREGATED_RESULT")) {
            int batchId = tuple.getActualTupleId();
            FilterAggregatorEngine.BatchInfo batch = engine.getBatch(batchId);
            if (batch != null) {
                // Exact arrival time at bedside display = fog arrival time + LAN latency (2 ms) + actuator latency (1 ms)
                double actuatorArrivalTime = CloudSim.clock() + SimulationConfig.LAN_LATENCY + SimulationConfig.ACTUATOR_LATENCY;
                MetricsTracker.getInstance().recordBatchDelivery(batch.constituentReadingIds, actuatorArrivalTime);
                return;
            }
        }

        super.processTupleArrival(ev);
    }

    @Override
    public void processEvent(SimEvent ev) {
        // Drain flush trailing aggregation buffers when sensor readings finish
        if (ev.getTag() == FogEvents.RESOURCE_MGMT) {
            if (!buffersFlushed && CloudSim.clock() >= (SimulationConfig.SIMULATION_DURATION + 500.0)) {
                buffersFlushed = true;
                List<FilterAggregatorEngine.BatchInfo> remainingBatches = engine.flushAllBuffers();
                for (FilterAggregatorEngine.BatchInfo batch : remainingBatches) {
                    dispatchAggregatedBatch(batch);
                }
            }
        }
        super.processEvent(ev);
    }

    private void dispatchAggregatedBatch(FilterAggregatorEngine.BatchInfo batch) {
        MetricsTracker.getInstance().recordAggregatedBatch(
                batch.constituentReadingIds, SimulationConfig.AGGREGATED_TUPLE_NW_SIZE, batch.batchId);

        // Instantiate aggregated summary tuple for cloud analytics
        Tuple batchTuple = new Tuple(
                SimulationConfig.APP_ID,
                FogUtils.generateTupleId(),
                Tuple.UP,
                SimulationConfig.AGGREGATED_CLOUD_CPU_LENGTH,
                1,
                SimulationConfig.AGGREGATED_TUPLE_NW_SIZE,
                3L,
                new UtilizationModelFull(),
                new UtilizationModelFull(),
                new UtilizationModelFull()
        );
        batchTuple.setUserId(brokerId);
        batchTuple.setTupleType("AGGREGATED_VITALS");
        batchTuple.setDestModuleName(SimulationConfig.MODULE_CLOUD_ANALYTICS);
        batchTuple.setSrcModuleName(SimulationConfig.MODULE_FOG_FILTER_AGGREGATOR);
        batchTuple.setActualTupleId(batch.batchId);

        // Send over the WAN link to Cloud
        sendUp(batchTuple);
    }

    public static FilteringFogDevice createFilteringDevice(String nodeName, long mips, int ram,
                                                           long upBw, long downBw, int level,
                                                           double ratePerMips, double busyPower, double idlePower,
                                                           FilterAggregatorEngine engine) {
        List<Pe> peList = new ArrayList<Pe>();
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000;
        int bw = 10000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(busyPower, idlePower)
        );

        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                "x86", "Linux", "Xen", host, 10.0, 3.0, 0.05, 0.001, 0.0);

        FilteringFogDevice fogdevice = null;
        try {
            fogdevice = new FilteringFogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), new LinkedList<Storage>(), 10, upBw, downBw, 0, ratePerMips, engine);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (fogdevice != null) {
            fogdevice.setLevel(level);
        }
        return fogdevice;
    }
}
