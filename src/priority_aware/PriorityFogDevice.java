package priority_aware;

import common.*;
import fog_filtering.FilterAggregatorEngine;
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
 * Priority-Aware Fog Device implementing dual-dispatch critical bypass,
 * expedited abnormal forwarding, and normal redundancy-filtered window aggregation.
 */
public class PriorityFogDevice extends HealthcareFogDevice {

    private final PriorityProcessorEngine engine;
    private int brokerId = 1;
    private boolean buffersFlushed = false;

    public PriorityFogDevice(String name, FogDeviceCharacteristics characteristics,
                             VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
                             double schedulingInterval, double uplinkBandwidth, double downlinkBandwidth,
                             double uplinkLatency, double ratePerMips, PriorityProcessorEngine engine) throws Exception {
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

        // Intercept incoming raw sensor readings at the Hospital Fog Gateway
        if (getName().equals(SimulationConfig.HOSPITAL_FOG_NAME) && tuple.getDirection() == Tuple.UP
                && !tuple.getTupleType().startsWith("AGGREGATED_") && !tuple.getTupleType().startsWith("CRITICAL_")
                && !tuple.getTupleType().startsWith("ABNORMAL_")) {

            int readingId = tuple.getActualTupleId();
            ReadingRecord rec = MetricsTracker.getInstance().getReading(readingId);

            if (rec != null) {
                List<FilterAggregatorEngine.BatchInfo> readyBatches = new ArrayList<>();
                PriorityProcessorEngine.PriorityDecision decision = engine.classifyAndRoute(rec, readyBatches);

                switch (decision) {
                    case CRITICAL_DUAL_DISPATCH:
                        // 1. Immediate Local Emergency Alert to bedside actuator (Zero aggregation wait)
                        double localDelay = SimulationConfig.LAN_LATENCY + SimulationConfig.ACTUATOR_LATENCY;
                        double alertTime = CloudSim.clock() + localDelay;
                        MetricsTracker.getInstance().recordCriticalImmediateDelivery(readingId, alertTime);

                        Tuple alertTuple = new Tuple(
                                SimulationConfig.APP_ID, FogUtils.generateTupleId(), Tuple.ACTUATOR,
                                SimulationConfig.ANALYTICS_RESULT_CPU_LENGTH, 1, SimulationConfig.ANALYTICS_RESULT_NW_SIZE,
                                3L, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull()
                        );
                        alertTuple.setUserId(brokerId);
                        alertTuple.setDestModuleName(SimulationConfig.ACTUATOR_DISPLAY);
                        alertTuple.setTupleType("CRITICAL_LOCAL_ALERT");
                        alertTuple.setActualTupleId(readingId);

                        int childGateway = getChildIdWithRouteTo(rec.getPatientGatewayId());
                        if (childGateway != -1) {
                            sendDown(alertTuple, childGateway);
                        }

                        // 2. Simultaneous immediate cloud dispatch for EHR logging
                        Tuple urgentTuple = new Tuple(
                                SimulationConfig.APP_ID, FogUtils.generateTupleId(), Tuple.UP,
                                SimulationConfig.CLOUD_ANALYTICS_CPU_LENGTH, 1, SimulationConfig.RAW_TUPLE_NW_SIZE,
                                3L, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull()
                        );
                        urgentTuple.setUserId(brokerId);
                        urgentTuple.setDestModuleName(SimulationConfig.MODULE_CLOUD_ANALYTICS);
                        urgentTuple.setSrcModuleName(SimulationConfig.MODULE_FOG_PRIORITY_PROCESSOR);
                        urgentTuple.setTupleType("CRITICAL_URGENT_VITALS");
                        urgentTuple.setActualTupleId(readingId);
                        sendUp(urgentTuple);
                        return;

                    case CRITICAL_ARTIFACT_SUPPRESSED:
                        MetricsTracker.getInstance().recordArtifactSuppressed(readingId);
                        return; // Motion artifact false alarm suppressed at fog!

                    case ABNORMAL_EXPEDITED:
                        // Immediate individual dispatch to cloud (bypasses aggregation window)
                        Tuple abnormalTuple = new Tuple(
                                SimulationConfig.APP_ID, FogUtils.generateTupleId(), Tuple.UP,
                                SimulationConfig.CLOUD_ANALYTICS_CPU_LENGTH, 1, SimulationConfig.RAW_TUPLE_NW_SIZE,
                                3L, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull()
                        );
                        abnormalTuple.setUserId(brokerId);
                        abnormalTuple.setDestModuleName(SimulationConfig.MODULE_CLOUD_ANALYTICS);
                        abnormalTuple.setSrcModuleName(SimulationConfig.MODULE_FOG_PRIORITY_PROCESSOR);
                        abnormalTuple.setTupleType("ABNORMAL_EXPEDITED_VITALS");
                        abnormalTuple.setActualTupleId(readingId);
                        sendUp(abnormalTuple);
                        return;

                    case NORMAL_FILTERED_DROPPED:
                        MetricsTracker.getInstance().recordFiltered(readingId);
                        return; // Dropped at fog!

                    case NORMAL_BUFFERED_IN_WINDOW:
                        return; // Buffered in window W

                    case NORMAL_BATCH_READY:
                        for (FilterAggregatorEngine.BatchInfo batch : readyBatches) {
                            dispatchAggregatedBatch(batch);
                        }
                        return;
                }
            }
        }

        // Intercept results returning DOWN from cloud
        if ((tuple.getDirection() == Tuple.DOWN || tuple.getDirection() == Tuple.ACTUATOR)
                && tuple.getTupleType().equals("AGGREGATED_RESULT")) {
            int batchId = tuple.getActualTupleId();
            FilterAggregatorEngine.BatchInfo batch = engine.getBatch(batchId);
            if (batch != null) {
                double actuatorArrivalTime = CloudSim.clock() + SimulationConfig.LAN_LATENCY + SimulationConfig.ACTUATOR_LATENCY;
                MetricsTracker.getInstance().recordBatchDelivery(batch.constituentReadingIds, actuatorArrivalTime);
                return;
            }
        }

        // Intercept abnormal/urgent results returning DOWN from cloud
        if ((tuple.getDirection() == Tuple.DOWN || tuple.getDirection() == Tuple.ACTUATOR)
                && tuple.getTupleType().equals("EXPEDITED_RESULT")) {
            int readingId = tuple.getActualTupleId();
            ReadingRecord r = MetricsTracker.getInstance().getReading(readingId);
            if (r != null && !r.isCriticalBypassed()) {
                double actuatorArrivalTime = CloudSim.clock() + SimulationConfig.LAN_LATENCY + SimulationConfig.ACTUATOR_LATENCY;
                MetricsTracker.getInstance().recordDelivery(readingId, actuatorArrivalTime);
            }
            return;
        }

        super.processTupleArrival(ev);
    }

    @Override
    public void processEvent(SimEvent ev) {
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

        Tuple batchTuple = new Tuple(
                SimulationConfig.APP_ID, FogUtils.generateTupleId(), Tuple.UP,
                SimulationConfig.AGGREGATED_CLOUD_CPU_LENGTH, 1, SimulationConfig.AGGREGATED_TUPLE_NW_SIZE,
                3L, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull()
        );
        batchTuple.setUserId(brokerId);
        batchTuple.setTupleType("AGGREGATED_VITALS");
        batchTuple.setDestModuleName(SimulationConfig.MODULE_CLOUD_ANALYTICS);
        batchTuple.setSrcModuleName(SimulationConfig.MODULE_FOG_PRIORITY_PROCESSOR);
        batchTuple.setActualTupleId(batch.batchId);

        sendUp(batchTuple);
    }

    public static PriorityFogDevice createPriorityDevice(String nodeName, long mips, int ram,
                                                         long upBw, long downBw, int level,
                                                         double ratePerMips, double busyPower, double idlePower,
                                                         PriorityProcessorEngine engine) {
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

        PriorityFogDevice fogdevice = null;
        try {
            fogdevice = new PriorityFogDevice(nodeName, characteristics,
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
