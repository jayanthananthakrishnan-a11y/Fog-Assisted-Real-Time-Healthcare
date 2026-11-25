package priority_aware;

import common.Severity;
import common.SimulationConfig;
import common.VitalSignType;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Random;

/**
 * TCP-Inspired Adaptive Sliding Window Controller (AIMD).
 * 
 * Implements Additive Increase Multiplicative Decrease (AIMD) for fog aggregation windows:
 * - Additive Increase: If consecutive readings remain stable (Normal/Delta within bounds)
 *   and edge fog queue utilization < 70%, increase window W <- min(W_max, W + 1).
 * - Multiplicative Decrease: If an abnormal or critical vital spike occurs, or queue > 80%,
 *   instantly halve window W <- max(1, floor(W / 2)) and flush buffered tuples.
 * 
 * Evaluated across 3 clinical patient profiles:
 * 1. Stable Post-Op (60%): W_max = 20 (high stationarity)
 * 2. Chronic Cardiac/COPD (25%): W_max = 8 (intermittent drift)
 * 3. Acute Unstable ICU (15%): W_max = 2 (volatile, rapid bypass)
 * 
 * Historical Horizons:
 * - 24-Hour Acute ICU Horizon (1,440 samples)
 * - 30-Day Chronic Ward Horizon (720 samples)
 */
public class AdaptiveTcpWindowController {

    public enum PatientProfile {
        STABLE_POST_OP("Stable Post-Op", 20, 0.94, 0.05, 0.01),
        CHRONIC_CARDIAC("Chronic Cardiac/COPD", 8, 0.82, 0.13, 0.05),
        ACUTE_UNSTABLE_ICU("Acute Unstable ICU", 2, 0.50, 0.30, 0.20);

        public final String label;
        public final int maxWindow;
        public final double probNormal;
        public final double probAbnormal;
        public final double probCritical;

        PatientProfile(String label, int maxWindow, double probNormal, double probAbnormal, double probCritical) {
            this.label = label;
            this.maxWindow = maxWindow;
            this.probNormal = probNormal;
            this.probAbnormal = probAbnormal;
            this.probCritical = probCritical;
        }
    }

    private final PatientProfile profile;
    private int currentWindow;
    private int consecutiveStableReadings = 0;
    private double lastHr = 75.0;
    private double lastSpo2 = 98.0;

    public AdaptiveTcpWindowController(PatientProfile profile) {
        this.profile = profile;
        this.currentWindow = Math.min(5, profile.maxWindow);
    }

    /**
     * Updates window size following TCP AIMD rules.
     */
    public int onReading(double hr, double spo2, Severity severity, double queueUtilPct) {
        boolean isDeltaStable = Math.abs(hr - lastHr) <= SimulationConfig.HR_DELTA_THRESHOLD &&
                                Math.abs(spo2 - lastSpo2) <= SimulationConfig.SPO2_DELTA_THRESHOLD;
        this.lastHr = hr;
        this.lastSpo2 = spo2;

        if (severity == Severity.CRITICAL || severity == Severity.ABNORMAL || queueUtilPct >= 75.0) {
            // Multiplicative Decrease (Halve or reset window to clear queue)
            currentWindow = Math.max(1, currentWindow / 2);
            consecutiveStableReadings = 0;
        } else if (isDeltaStable && queueUtilPct < 70.0) {
            consecutiveStableReadings++;
            // Additive Increase every 2 consecutive stable readings
            if (consecutiveStableReadings >= 2) {
                currentWindow = Math.min(profile.maxWindow, currentWindow + 1);
                consecutiveStableReadings = 0;
            }
        }
        return currentWindow;
    }

    public int getCurrentWindow() {
        return currentWindow;
    }

    public PatientProfile getProfile() {
        return profile;
    }

    /**
     * Generates Experiment 6 Dataset across 24-Hour and 30-Day horizons.
     */
    public static void generateExp6Dataset(String csvPath) {
        try {
            File f = new File(csvPath);
            if (f.getParentFile() != null) f.getParentFile().mkdirs();

            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println("profile,horizon,sample_idx,time_s,vital_hr,vital_spo2,vital_bp,vital_temp,event_type,window_size_w,queue_util_pct,decision");

                Random rnd = new Random(SimulationConfig.RANDOM_SEED);

                for (PatientProfile prof : PatientProfile.values()) {
                    // 1. 24-Hour Acute ICU Horizon (1,440 minute-level steps)
                    runProfileHorizon(pw, prof, "24h_acute_icu", 1440, 60.0, rnd);

                    // 2. 30-Day Chronic Ward Horizon (720 hourly steps)
                    runProfileHorizon(pw, prof, "30d_chronic_ward", 720, 3600.0, rnd);
                }
            }
            System.out.println("[Experiment 6] Generated Adaptive TCP Window Dataset: " + csvPath);
        } catch (Exception e) {
            System.err.println("[Experiment 6] Error generating CSV: " + e.getMessage());
        }
    }

    private static void runProfileHorizon(PrintWriter pw, PatientProfile prof, String horizon,
                                          int steps, double intervalSeconds, Random rnd) {
        AdaptiveTcpWindowController ctrl = new AdaptiveTcpWindowController(prof);

        double baseHr = 72.0;
        double baseSpo2 = 98.0;
        double baseBp = 120.0;
        double baseTemp = 37.0;

        for (int i = 1; i <= steps; i++) {
            double timeSeconds = i * intervalSeconds;
            double r = rnd.nextDouble();
            Severity sev;
            String eventType;

            if (r < prof.probNormal) {
                sev = Severity.NORMAL;
                eventType = "ROUTINE_NORMAL";
            } else if (r < prof.probNormal + prof.probAbnormal) {
                sev = Severity.ABNORMAL;
                eventType = "ABNORMAL_DRIFT";
            } else {
                sev = Severity.CRITICAL;
                eventType = "CRITICAL_SPIKE";
            }

            // Generate values with small drift
            double hr = baseHr + (rnd.nextDouble() * 10.0 - 5.0);
            double spo2 = baseSpo2 + (rnd.nextDouble() * 2.0 - 1.0);
            double bp = baseBp + (rnd.nextDouble() * 12.0 - 6.0);
            double temp = baseTemp + (rnd.nextDouble() * 0.4 - 0.2);

            if (sev == Severity.ABNORMAL) {
                hr += 25.0;
                spo2 -= 5.0;
            } else if (sev == Severity.CRITICAL) {
                hr += 60.0; // Ventricular Tachycardia
                spo2 -= 14.0; // Hypoxemia
            }

            // Simulate edge queue utilization
            double queueUtil = 20.0 + (rnd.nextDouble() * 30.0);
            if (sev != Severity.NORMAL) queueUtil += 35.0;

            int prevW = ctrl.getCurrentWindow();
            int newW = ctrl.onReading(hr, spo2, sev, queueUtil);

            String decision;
            if (newW < prevW) decision = "MULTIPLICATIVE_DECREASE_FLUSH";
            else if (newW > prevW) decision = "ADDITIVE_INCREASE";
            else decision = "WINDOW_STABLE";

            pw.printf(Locale.US, "%s,%s,%d,%.1f,%.1f,%.1f,%.1f,%.2f,%s,%d,%.1f,%s\n",
                    prof.label, horizon, i, timeSeconds, hr, spo2, bp, temp, eventType, newW, queueUtil, decision);
        }
    }
}
