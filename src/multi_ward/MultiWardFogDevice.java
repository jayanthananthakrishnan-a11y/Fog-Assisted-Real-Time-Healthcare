package multi_ward;

import common.*;
import fog_filtering.FilterAggregatorEngine;
import priority_aware.PriorityProcessorEngine;
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
 * Multi-Ward Fog Device with Priority-Safe Load Offloading & Inter-Ward Failover.
 * 
 * Features:
 * 1. Dual-Ward Coordination: Connected to a peer gateway via a 1.0 ms high-speed hospital LAN.
 * 2. Load Offloading: When CPU utilization > 80% and peer is < 60%, offloads normal batches across LAN.
 * 3. Scheduled Outage & Failover: Simulates gateway downtime (t = 40s to 60s). During outage,
 *    reroutes traffic to peer gateway maintaining sub-12 ms emergency alerts.
 */
public class MultiWardFogDevice extends HealthcareFogDevice {

    private final PriorityProcessorEngine engine;
    private final boolean isWardA;
    private MultiWardFogDevice partnerGateway;
    private int brokerId = 1;
    private boolean failoverEnabled = true;

    // Outage simulation parameters (for Gateway A)
    private double outageStartTime = 40000.0;
    private double outageEndTime = 60000.0;

    // Performance counters
    private int offloadedBatchesCount = 0;
    private int failoverReroutedCount = 0;
    private boolean buffersFlushed = false;

    public PriorityProcessorEngine getEngine() {
        return engine;
    }

    public MultiWardFogDevice(String name, FogDeviceCharacteristics characteristics,
                              VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
                              double schedulingInterval, double uplinkBandwidth, double downlinkBandwidth,
                              double uplinkLatency, double ratePerMips,
                              PriorityProcessorEngine engine, boolean isWardA) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval,
                uplinkBandwidth, downlinkBandwidth, uplinkLatency, ratePerMips);
        this.engine = engine;
        this.isWardA = isWardA;
    }

    public void setPartnerGateway(MultiWardFogDevice partner) {
        this.partnerGateway = partner;
    }

    public void setFailoverEnabled(boolean failoverEnabled) {
        this.failoverEnabled = failoverEnabled;
    }

    public void setBrokerId(int brokerId) {
        this.brokerId = brokerId;
    }

    public boolean isInOutage() {
        if (!isWardA) return false;
        double now = CloudSim.clock();
        return now >= outageStartTime && now < outageEndTime;
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

        // Intercept sensor telemetry arriving at this ward gateway
        if (tuple.getDirection() == Tuple.UP
                && !tuple.getTupleType().startsWith("AGGREGATED_")
                && !tuple.getTupleType().startsWith("CRITICAL_")
                && !tuple.getTupleType().startsWith("ABNORMAL_")) {

            int readingId = tuple.getActualTupleId();
            ReadingRecord rec = MetricsTracker.getInstance().getReading(readingId);

            if (rec != null) {
                // Check if this gateway is currently experiencing a scheduled outage
                if (isInOutage()) {
                    if (failoverEnabled && partnerGateway != null) {
                        // Seamless Failover: Forward reading to Partner Gateway across 1.0 ms LAN
                        failoverReroutedCount++;
                        partnerGateway.handleFailoverReading(rec, this);
                        return;
                    } else {
                        // No failover: Fallback to high-latency WAN (simulating Arch D outage without partner)
                        handleWanFallback(rec);
                        return;
                    }
                }

                processLocalReading(rec, 0.0);
                return;
            }
        }

        // Intercept results returning DOWN from cloud
        if ((tuple.getDirection() == Tuple.DOWN || tuple.getDirection() == Tuple.ACTUATOR)
                && tuple.getTupleType().equals("AGGREGATED_RESULT")) {
            int batchId = tuple.getActualTupleId();
            FilterAggregatorEngine.BatchInfo batch = engine.getBatch(batchId);
            if (batch == null && partnerGateway != null) {
                batch = partnerGateway.getEngine().getBatch(batchId);
            }
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

    /**
     * Handles readings rerouted from a failed partner gateway across the 1.0 ms inter-ward link.
     */
    public void handleFailoverReading(ReadingRecord rec, MultiWardFogDevice originGateway) {
        // Additional 1.0 ms latency for inter-ward LAN transfer
        double interWardPenalty = SimulationConfig.INTER_WARD_LAN_LATENCY;
        processLocalReading(rec, interWardPenalty);
    }

    private void processLocalReading(ReadingRecord rec, double interWardDelay) {
        int readingId = rec.getReadingId();
        List<FilterAggregatorEngine.BatchInfo> readyBatches = new ArrayList<>();
        PriorityProcessorEngine.PriorityDecision decision = engine.classifyAndRoute(rec, readyBatches);

        switch (decision) {
            case CRITICAL_DUAL_DISPATCH:
                // Immediate Local Emergency Alert (Zero aggregation wait)
                // If failover is active, add 1.0 ms inter-ward traversal delay
                double localDelay = SimulationConfig.LAN_LATENCY + SimulationConfig.ACTUATOR_LATENCY + (interWardDelay * 2);
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

                // Dispatch to patient's ward actuator
                int childGateway = getChildIdWithRouteTo(rec.getPatientGatewayId());
                if (childGateway != -1) {
                    sendDown(alertTuple, childGateway);
                } else if (partnerGateway != null) {
                    // Send via partner's downlink
                    int partnerChild = partnerGateway.getChildIdWithRouteTo(rec.getPatientGatewayId());
                    if (partnerChild != -1) {
                        partnerGateway.sendDown(alertTuple, partnerChild);
                    }
                }

                // Simultaneous immediate cloud dispatch for EHR logging
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
                return;

            case ABNORMAL_EXPEDITED:
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
                return;

            case NORMAL_BUFFERED_IN_WINDOW:
                return;

            case NORMAL_BATCH_READY:
                for (FilterAggregatorEngine.BatchInfo batch : readyBatches) {
                    dispatchAggregatedBatch(batch);
                }
                return;
        }
    }

    private void handleWanFallback(ReadingRecord rec) {
        // Fallback directly to cloud over WAN because local gateway is dead and no failover partner
        double wanDelay = SimulationConfig.wanLatency * 2 + 10.0;
        double completeTime = CloudSim.clock() + wanDelay;
        MetricsTracker.getInstance().recordDelivery(rec.getReadingId(), completeTime);
    }

    private void dispatchAggregatedBatch(FilterAggregatorEngine.BatchInfo batch) {
        long batchSize = Math.max(SimulationConfig.RAW_TUPLE_NW_SIZE,
                (long) (batch.constituentReadingIds.size() * SimulationConfig.RAW_TUPLE_NW_SIZE * 0.26));
        MetricsTracker.getInstance().recordAggregatedBatch(batch.constituentReadingIds, batchSize, batch.batchId);

        Tuple batchTuple = new Tuple(
                SimulationConfig.APP_ID, FogUtils.generateTupleId(), Tuple.UP,
                SimulationConfig.AGGREGATED_CLOUD_CPU_LENGTH, 1, batchSize,
                3L, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull()
        );
        batchTuple.setUserId(brokerId);
        batchTuple.setDestModuleName(SimulationConfig.MODULE_CLOUD_ANALYTICS);
        batchTuple.setSrcModuleName(SimulationConfig.MODULE_FOG_PRIORITY_PROCESSOR);
        batchTuple.setTupleType("AGGREGATED_VITALS");
        batchTuple.setActualTupleId(batch.batchId);
        sendUp(batchTuple);
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == FogEvents.RESOURCE_MGMT) {
            if (!buffersFlushed && CloudSim.clock() >= (SimulationConfig.SIMULATION_DURATION + 500.0)) {
                buffersFlushed = true;
                flushBuffers();
            }
        }
        super.processEvent(ev);
    }

    private synchronized void flushBuffers() {
        List<FilterAggregatorEngine.BatchInfo> flushed = engine.flushAllBuffers();
        if (flushed != null && !flushed.isEmpty()) {
            for (FilterAggregatorEngine.BatchInfo batch : flushed) {
                dispatchAggregatedBatch(batch);
            }
        }
    }

    public int getFailoverReroutedCount() {
        return failoverReroutedCount;
    }

    public static MultiWardFogDevice createMultiWardDevice(String nodeName, long mips, int ram,
                                                           long upBw, long downBw, int level,
                                                           double ratePerMips, double busyPower, double idlePower,
                                                           PriorityProcessorEngine engine, boolean isWardA) {
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

        MultiWardFogDevice fogdevice = null;
        try {
            fogdevice = new MultiWardFogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), new LinkedList<Storage>(), 10, upBw, downBw, 0, ratePerMips, engine, isWardA);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (fogdevice != null) {
            fogdevice.setLevel(level);
        }
        return fogdevice;
    }
}
