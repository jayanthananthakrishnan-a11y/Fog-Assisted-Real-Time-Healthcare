package priority_aware;

import common.Severity;
import common.VitalSignType;
import java.util.HashMap;
import java.util.Map;

/**
 * Formal Sensor-to-Actuator Directed Acyclic Graph (DAG) Model & NEWS2 Risk Fusion.
 * 
 * Topology:
 * Level 0: 6 Physical Sensors (HR, SpO2, Temp, BP, Resp, ECG_HRV)
 *    |
 *    v
 * Level 1: Edge Delta-Filter & Artifact Verifier (suppresses sensor motion artifacts)
 *    |
 *    v
 * Level 2: Multi-Vital NEWS2 Composite Risk Fusion (evaluates multi-organ decompensation)
 *    |
 *    +------------------------+
 *    |                        |
 *    v                        v
 * Level 3a: Bedside Monitor   Level 3b: Cloud EHR Analytics
 * (Emergency Alert < 10 ms)   (Longitudinal Storage over WAN)
 */
public class HealthcareDagModel {

    public static class ClinicalReadingSet {
        public double heartRate = 75.0;
        public double spo2 = 98.0;
        public double temperature = 37.0;
        public double bloodPressure = 120.0;
        public double respirationRate = 16.0;
        public double ecgHrv = 55.0; // ms SDNN
        public boolean patientMoving = false; // Sensor accelerometer motion flag
    }

    public static class News2Evaluation {
        public final int score;
        public final String riskCategory; // LOW, MEDIUM, HIGH
        public final boolean requiresEmergencyAlert;
        public final boolean isArtifactSuppressed;

        public News2Evaluation(int score, String riskCategory, boolean requiresEmergencyAlert, boolean isArtifactSuppressed) {
            this.score = score;
            this.riskCategory = riskCategory;
            this.requiresEmergencyAlert = requiresEmergencyAlert;
            this.isArtifactSuppressed = isArtifactSuppressed;
        }
    }

    /**
     * Computes formal National Early Warning Score (NEWS2) across patient vital signs.
     */
    public static int calculateNews2Score(double hr, double spo2, double temp, double sbp, double resp) {
        int score = 0;

        // 1. Respiration Rate (breaths/min)
        if (resp <= 8 || resp >= 25) score += 3;
        else if (resp >= 21) score += 2;
        else if (resp >= 9 && resp <= 11) score += 1;

        // 2. SpO2 Scale 1 (%)
        if (spo2 <= 91) score += 3;
        else if (spo2 <= 93) score += 2;
        else if (spo2 <= 95) score += 1;

        // 3. Systolic Blood Pressure (mmHg)
        if (sbp <= 90 || sbp >= 220) score += 3;
        else if (sbp <= 100) score += 2;
        else if (sbp <= 110) score += 1;

        // 4. Heart Rate (BPM)
        if (hr <= 40 || hr >= 131) score += 3;
        else if (hr >= 111) score += 2;
        else if (hr <= 50 || hr >= 91) score += 1;

        // 5. Temperature (°C)
        if (temp <= 35.0) score += 3;
        else if (temp >= 39.1) score += 2;
        else if (temp <= 36.0 || temp >= 38.1) score += 1;

        return score;
    }

    /**
     * Executes the Level 1 -> Level 2 DAG Pipeline:
     * Delta-Filtering -> Artifact Verification -> NEWS2 Fusion
     */
    public static News2Evaluation evaluateDag(ClinicalReadingSet vitals, boolean consecutiveCritical) {
        int newsScore = calculateNews2Score(vitals.heartRate, vitals.spo2, vitals.temperature, vitals.bloodPressure, vitals.respirationRate);

        // Check if a single sensor has an isolated spike while others are normal
        boolean singleSpike = (vitals.heartRate > 130 || vitals.heartRate < 45) && vitals.spo2 >= 95 && vitals.bloodPressure >= 105;
        boolean motionArtifact = singleSpike && !consecutiveCritical;

        String riskCategory;
        boolean emergency;
        boolean suppressed = false;

        if (motionArtifact) {
            suppressed = true;
            riskCategory = "ARTIFACT_SUPPRESSED";
            emergency = false;
        } else if (newsScore >= 7 || vitals.heartRate >= 140 || vitals.spo2 < 88) {
            riskCategory = "HIGH_EMERGENCY";
            emergency = true;
        } else if (newsScore >= 5) {
            riskCategory = "MEDIUM_ALERT";
            emergency = false;
        } else {
            riskCategory = "LOW_STABLE";
            emergency = false;
        }

        return new News2Evaluation(newsScore, riskCategory, emergency, suppressed);
    }
}
