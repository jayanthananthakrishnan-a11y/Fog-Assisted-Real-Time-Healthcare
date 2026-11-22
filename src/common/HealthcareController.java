package common;

import java.util.List;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.Controller;
import org.fog.utils.FogEvents;

/**
 * Controller subclass that intercepts simulation termination to print comprehensive
 * experimental metrics and export raw & summary CSV files.
 */
public class HealthcareController extends Controller {

    private String readingsCsvPath = SimulationConfig.READINGS_CSV_PATH;
    private String summaryCsvPath = SimulationConfig.SUMMARY_CSV_PATH;

    public HealthcareController(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators) {
        super(name, fogDevices, sensors, actuators);
    }

    public HealthcareController(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
                                String readingsCsvPath, String summaryCsvPath) {
        super(name, fogDevices, sensors, actuators);
        this.readingsCsvPath = readingsCsvPath;
        this.summaryCsvPath = summaryCsvPath;
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == FogEvents.STOP_SIMULATION) {
            MetricsTracker.getInstance().printSummary();
            MetricsTracker.getInstance().exportToCsv(readingsCsvPath, summaryCsvPath);
            super.processEvent(ev);
        } else {
            super.processEvent(ev);
        }
    }
}
