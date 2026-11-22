package common;

/**
 * Centralized simulation parameters and constants for Topic 19 Healthcare Experiments.
 * All parameters are configured here to ensure 100% reproducibility across all architectures.
 */
public class SimulationConfig {

    // --- Workload & Patient Configuration ---
    public static int numPatients = 10; // Default 10 patients (configurable via CLI args)
    public static final int SENSORS_PER_PATIENT = 4;
    public static final double READING_INTERVAL = 1000.0; // 1 reading/sec per sensor (ms)
    public static final int READINGS_PER_SENSOR = 100; // 100 readings per sensor
    public static final double TRANSMISSION_START_DELAY = 50.0; // Initial startup delay (ms)
    public static final double SIMULATION_DURATION = 100000.0; // 100 seconds (ms)
    public static final int STOP_SIMULATION_TIME = 115000; // 15s drain buffer to allow all in-flight flushed batches to complete (ms)
    public static final double CRITICAL_SLA_DEADLINE_MS = 500.0; // 500 ms emergency SLA deadline for clinical alerts

    // --- Random Seeding & Severity Target Distribution ---
    public static final long RANDOM_SEED = 42L;
    public static final double NORMAL_PROBABILITY = 0.85;  // ~85% Normal readings
    public static final double ABNORMAL_PROBABILITY = 0.10; // ~10% Abnormal readings
    public static final double CRITICAL_PROBABILITY = 0.05; // ~5% Critical readings
    public static boolean enableArtifactSuppression = true; // Cross-sensor false alarm suppression

    // --- Filtering Delta Thresholds (Architecture C & D) ---
    public static final double HR_DELTA_THRESHOLD = 2.0;    // BPM
    public static final double SPO2_DELTA_THRESHOLD = 1.0;  // %
    public static final double TEMP_DELTA_THRESHOLD = 0.2;  // °C
    public static final double BP_DELTA_THRESHOLD = 3.0;    // mmHg

    // --- Aggregation Parameters (Architecture C & D) ---
    public static int aggregationWindowSize = 5; // Default W = 5 readings (configurable via CLI)
    public static final long AGGREGATED_TUPLE_NW_SIZE = 650; // Bytes per aggregated batch
    public static final long AGGREGATED_CLOUD_CPU_LENGTH = 2500; // MIPS computation demand for batch

    // --- Fog Module Computation Demands ---
    public static final long FOG_FORWARD_CPU_LENGTH = 200; // MIPS for fog forwarder inspection (Arch B)
    public static final long FOG_FILTER_CPU_LENGTH = 150;  // MIPS for filter & aggregator (Arch C)
    public static final long FOG_PRIORITY_CPU_LENGTH = 50; // MIPS for priority classifier (Arch D)

    // --- Tuple Computation & Network Demands (Raw) ---
    public static final long RAW_TUPLE_NW_SIZE = 500; // Bytes (0.5 KB per raw reading)
    public static final long CLOUD_ANALYTICS_CPU_LENGTH = 2000; // MIPS computation demand
    public static final long ANALYTICS_RESULT_NW_SIZE = 200; // Bytes (downlink result to actuator)
    public static final long ANALYTICS_RESULT_CPU_LENGTH = 100; // MIPS to process on actuator

    // --- Baseline Reference Constants (for Bandwidth Saving calculations) ---
    public static final long BASELINE_CLOUD_TRAFFIC_BYTES = 2000000L; // 2,000,000 Bytes (Architecture A)
    public static final long BASELINE_TOTAL_TRAFFIC_BYTES = 5600000L; // 5,600,000 Bytes (Architecture A)

    // --- Physical Topology & Link Latencies ---
    // Cloud Datacenter (Level 0 Root)
    public static final String CLOUD_NAME = "cloud";
    public static final long CLOUD_MIPS = 44800; // MIPS
    public static final int CLOUD_RAM = 40000; // MB
    public static final long CLOUD_STORAGE = 1000000; // MB
    public static final double CLOUD_UP_BW = 10000.0; // Mbps
    public static final double CLOUD_DOWN_BW = 10000.0; // Mbps
    public static final double CLOUD_RATE_PER_MIPS = 0.01;
    public static final double CLOUD_BUSY_POWER = 1648.0; // Watts
    public static final double CLOUD_IDLE_POWER = 1332.0; // Watts

    // WAN Link (Hospital Fog Gateway to Cloud)
    public static double wanLatency = 100.0; // Remote cloud propagation latency (ms) (configurable)
    public static double wanBandwidth = 10000.0; // WAN link bandwidth in Mbps or Kbps (configurable)
    public static final double WAN_LATENCY = 100.0; // Default constant for backwards compatibility

    // Hospital Fog Gateway (Level 1 Shared Ward Node for Architectures B, C, D)
    public static final String HOSPITAL_FOG_NAME = "hospital_fog_gateway";
    public static final String FOG_GATEWAY_WARD_A_NAME = "fog_gateway_ward_a";
    public static final String FOG_GATEWAY_WARD_B_NAME = "fog_gateway_ward_b";
    public static final double INTER_WARD_LAN_LATENCY = 1.0; // 1.0 ms high-speed switched inter-ward link
    public static final long HOSPITAL_FOG_MIPS = 8000; // MIPS
    public static final int HOSPITAL_FOG_RAM = 8000; // MB
    public static final long HOSPITAL_FOG_STORAGE = 1000000; // MB
    public static final double HOSPITAL_FOG_UP_BW = 10000.0; // Mbps
    public static final double HOSPITAL_FOG_DOWN_BW = 10000.0; // Mbps
    public static final double HOSPITAL_FOG_RATE_PER_MIPS = 0.0;
    public static final double HOSPITAL_FOG_BUSY_POWER = 107.339; // Watts
    public static final double HOSPITAL_FOG_IDLE_POWER = 83.4333; // Watts

    // Local Hospital LAN Link (Patient Bridge to Hospital Fog Gateway)
    public static final double LAN_LATENCY = 2.0; // Hospital LAN propagation latency (ms)

    // Patient Gateway (Level 2 Local Patient Bridge)
    public static final String GATEWAY_PREFIX = "patient_gateway_";
    public static final long GATEWAY_MIPS = 2800; // MIPS
    public static final int GATEWAY_RAM = 4000; // MB
    public static final long GATEWAY_STORAGE = 100000; // MB
    public static final double GATEWAY_UP_BW = 10000.0; // Mbps
    public static final double GATEWAY_DOWN_BW = 10000.0; // Mbps
    public static final double GATEWAY_RATE_PER_MIPS = 0.0;
    public static final double GATEWAY_BUSY_POWER = 107.339; // Watts
    public static final double GATEWAY_IDLE_POWER = 83.4333; // Watts

    // Local Wireless Edge Links
    public static final double SENSOR_LATENCY = 2.0; // BLE / Zigbee to patient gateway (ms)
    public static final double ACTUATOR_LATENCY = 1.0; // Local display link (ms)

    // --- Application Architecture ---
    public static final String APP_ID = "HealthcareMonitoringApp";
    public static final String MODULE_CLOUD_ANALYTICS = "cloud_analytics";
    public static final int MODULE_CLOUD_ANALYTICS_RAM = 1024; // MB
    public static final String MODULE_FOG_FORWARDER = "fog_forwarder";
    public static final String MODULE_FOG_FILTER_AGGREGATOR = "fog_filter_aggregator";
    public static final String MODULE_FOG_PRIORITY_PROCESSOR = "fog_priority_processor";
    public static final int MODULE_FOG_RAM = 512; // MB
    public static final String ACTUATOR_DISPLAY = "client_display";
    public static final String TUPLE_ANALYTICS_RESULT = "ANALYTICS_RESULT";

    // --- Result Export Paths ---
    // Architecture A
    public static final String READINGS_CSV_PATH_A = "results/raw/arch_a_baseline_readings.csv";
    public static final String SUMMARY_CSV_PATH_A = "results/csv/arch_a_baseline_summary.csv";
    // Architecture B
    public static final String READINGS_CSV_PATH_B = "results/raw/arch_b_forwarding_readings.csv";
    public static final String SUMMARY_CSV_PATH_B = "results/csv/arch_b_forwarding_summary.csv";
    // Architecture C
    public static final String READINGS_CSV_PATH_C = "results/raw/arch_c_filtering_readings.csv";
    public static final String SUMMARY_CSV_PATH_C = "results/csv/arch_c_filtering_summary.csv";
    // Architecture D
    public static final String READINGS_CSV_PATH_D = "results/raw/arch_d_priority_readings.csv";
    public static final String SUMMARY_CSV_PATH_D = "results/csv/arch_d_priority_summary.csv";
    // Architecture E (Multi-Ward Two-Gateway Failover)
    public static final String READINGS_CSV_PATH_E = "results/raw/arch_e_multiward_readings.csv";
    public static final String SUMMARY_CSV_PATH_E = "results/csv/arch_e_multiward_summary.csv";

    // Backward-compatibility aliases for Architecture A
    public static final String READINGS_CSV_PATH = READINGS_CSV_PATH_A;
    public static final String SUMMARY_CSV_PATH = SUMMARY_CSV_PATH_A;

    public static void setNumPatients(int n) {
        if (n > 0) {
            numPatients = n;
        }
    }

    public static void setWindowSize(int w) {
        if (w > 0) {
            aggregationWindowSize = w;
        }
    }

    public static void setWanLatency(double lat) {
        if (lat > 0) {
            wanLatency = lat;
        }
    }

    public static void setWanBandwidth(double bw) {
        if (bw > 0) {
            wanBandwidth = bw;
        }
    }

    public static void parseArgs(String[] args) {
        if (args != null && args.length > 0) {
            try {
                if (args.length >= 1 && !args[0].isEmpty()) {
                    setNumPatients(Integer.parseInt(args[0]));
                }
                if (args.length >= 2 && !args[1].isEmpty()) {
                    setWindowSize(Integer.parseInt(args[1]));
                }
                if (args.length >= 3 && !args[2].isEmpty()) {
                    setWanLatency(Double.parseDouble(args[2]));
                }
                if (args.length >= 4 && !args[3].isEmpty()) {
                    setWanBandwidth(Double.parseDouble(args[3]));
                }
            } catch (Exception e) {
                System.err.println("[Config] Error parsing CLI arguments: " + e.getMessage());
            }
        }
    }
}
