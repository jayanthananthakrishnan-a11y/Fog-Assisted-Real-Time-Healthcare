package common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Tuple;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogEvents;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;

/**
 * Custom FogDevice extending iFogSim's core FogDevice with healthcare
 * cloud-arrival metrics recording and targeted downstream actuator routing.
 */
public class HealthcareFogDevice extends FogDevice {

    public HealthcareFogDevice(String name, FogDeviceCharacteristics characteristics,
                               VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
                               double schedulingInterval, double uplinkBandwidth, double downlinkBandwidth,
                               double uplinkLatency, double ratePerMips) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval,
                uplinkBandwidth, downlinkBandwidth, uplinkLatency, ratePerMips);
    }

    @Override
    protected void processTupleArrival(SimEvent ev) {
        Tuple tuple = (Tuple) ev.getData();
        if (getName().equals(SimulationConfig.CLOUD_NAME) && tuple.getDirection() == Tuple.UP) {
            // Only record cloud arrival for raw readings (not aggregated batches which use batchId)
            if (!tuple.getTupleType().equals("AGGREGATED_VITALS")) {
                MetricsTracker.getInstance().recordCloudArrival(tuple.getActualTupleId(), CloudSim.clock());
            }
        }
        super.processTupleArrival(ev);
    }

    @Override
    protected void sendTupleToActuator(Tuple tuple) {
        for (Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()) {
            int actuatorId = actuatorAssociation.getFirst();
            double delay = actuatorAssociation.getSecond();
            String actuatorType = ((Actuator) CloudSim.getEntity(actuatorId)).getActuatorType();
            if (tuple.getDestModuleName().equals(actuatorType)) {
                send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
                return;
            }
        }

        // Targeted downstream routing: route to the child gateway that hosts the originating patient
        ReadingRecord rec = MetricsTracker.getInstance().getReading(tuple.getActualTupleId());
        if (rec != null) {
            int targetGatewayId = rec.getPatientGatewayId();
            int childId = getChildIdWithRouteTo(targetGatewayId);
            if (childId != -1) {
                sendDown(tuple, childId);
                return;
            }
        }

        super.sendTupleToActuator(tuple);
    }

    /**
     * Factory method to construct configured HealthcareFogDevice instances.
     */
    public static HealthcareFogDevice createDevice(String nodeName, long mips, int ram,
                                                   long upBw, long downBw, int level,
                                                   double ratePerMips, double busyPower, double idlePower) {
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

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        HealthcareFogDevice fogdevice = null;
        try {
            fogdevice = new HealthcareFogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (fogdevice != null) {
            fogdevice.setLevel(level);
        }
        return fogdevice;
    }
}
