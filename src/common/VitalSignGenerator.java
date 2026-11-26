package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * High-fidelity vital signs provider.
 * Loads and replays genuine hospital ICU patient trajectories from the
 * PhysioNet BIDMC & MIMIC-III Demo datasets (data/processed_icu/icu_cohort_24h_acute.csv).
 * Falls back to deterministic seed-based mathematical generation if the CSV is not found.
 */
public class VitalSignGenerator {

    private final Random random;
    private static final Map<String, List<RealPatientRecord>> patientCache = new HashMap<>();
    private static boolean realDataLoaded = false;

    private static class RealPatientRecord {
        double hr, spo2, temp, bp, resp, ecgHrv;
        Severity severity;
        int news2;
        boolean isArtifact;

        RealPatientRecord(double hr, double spo2, double temp, double bp, double resp, double ecgHrv, Severity severity, int news2, boolean isArtifact) {
            this.hr = hr;
            this.spo2 = spo2;
            this.temp = temp;
            this.bp = bp;
            this.resp = resp;
            this.ecgHrv = ecgHrv;
            this.severity = severity;
            this.news2 = news2;
            this.isArtifact = isArtifact;
        }
    }

    public VitalSignGenerator() {
        this.random = new Random(SimulationConfig.RANDOM_SEED);
        loadRealIcuDataset();
    }

    public VitalSignGenerator(long seed) {
        this.random = new Random(seed);
        loadRealIcuDataset();
    }

    private synchronized void loadRealIcuDataset() {
        if (realDataLoaded) return;
        File csvFile = new File("data/processed_icu/icu_cohort_24h_acute.csv");
        if (!csvFile.exists()) {
            csvFile = new File("../data/processed_icu/icu_cohort_24h_acute.csv");
        }
        if (!csvFile.exists()) {
            realDataLoaded = true;
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 11) {
                    // timestamp, patient_id, profile, heart_rate, spo2, temperature, blood_pressure, respiration, ecg_hrv, severity, news2_score, is_artifact
                    String pid = parts[1].trim();
                    double hr = Double.parseDouble(parts[3]);
                    double spo2 = Double.parseDouble(parts[4]);
                    double temp = Double.parseDouble(parts[5]);
                    double bp = Double.parseDouble(parts[6]);
                    double resp = Double.parseDouble(parts[7]);
                    double ecgHrv = Double.parseDouble(parts[8]);
                    String sevStr = parts[9].trim().toUpperCase();
                    Severity sev = Severity.NORMAL;
                    if (sevStr.contains("CRIT")) sev = Severity.CRITICAL;
                    else if (sevStr.contains("ABN")) sev = Severity.ABNORMAL;
                    int news2 = Integer.parseInt(parts[10]);
                    boolean isArt = parts.length > 11 && parts[11].trim().equals("1");

                    patientCache.computeIfAbsent(pid, k -> new ArrayList<>())
                            .add(new RealPatientRecord(hr, spo2, temp, bp, resp, ecgHrv, sev, news2, isArt));
                }
            }
            realDataLoaded = true;
            System.out.println("[VitalSignGenerator] Successfully loaded " + patientCache.size() + " real PhysioNet ICU patient trajectories into memory.");
        } catch (Exception e) {
            System.err.println("[VitalSignGenerator] Error reading PhysioNet dataset, using mathematical fallback: " + e.getMessage());
            realDataLoaded = true;
        }
    }

    /**
     * Generates a reading for a specific patient and sensor type, replaying real PhysioNet data when available.
     */
    public ReadingRecord generateReading(int readingId, int patientId, int gatewayId, String sensorType, double generateTime) {
        String pidKey = String.format("ICU_PATIENT_%02d", (patientId % 53) + 1);
        List<RealPatientRecord> records = patientCache.get(pidKey);

        if (records != null && !records.isEmpty()) {
            int idx = Math.abs(readingId) % records.size();
            RealPatientRecord rec = records.get(idx);
            double val = getVitalValue(sensorType, rec);
            Severity sev = rec.severity;
            return new ReadingRecord(readingId, patientId, gatewayId, sensorType, val, sev, generateTime, SimulationConfig.RAW_TUPLE_NW_SIZE);
        }

        // Fallback to deterministic generator
        double r = random.nextDouble();
        Severity severity;
        if (r < SimulationConfig.NORMAL_PROBABILITY) {
            severity = Severity.NORMAL;
        } else if (r < SimulationConfig.NORMAL_PROBABILITY + SimulationConfig.ABNORMAL_PROBABILITY) {
            severity = Severity.ABNORMAL;
        } else {
            severity = Severity.CRITICAL;
        }

        double value = generateFallbackValue(sensorType, severity);
        return new ReadingRecord(readingId, patientId, gatewayId, sensorType, value, severity, generateTime, SimulationConfig.RAW_TUPLE_NW_SIZE);
    }

    private double getVitalValue(String sensorType, RealPatientRecord rec) {
        switch (sensorType) {
            case VitalSignType.HEART_RATE: return rec.hr;
            case VitalSignType.SPO2: return rec.spo2;
            case VitalSignType.TEMPERATURE: return rec.temp;
            case VitalSignType.BLOOD_PRESSURE: return rec.bp;
            case VitalSignType.RESPIRATION: return rec.resp;
            case VitalSignType.ECG_HRV: return rec.ecgHrv;
            default: return rec.hr;
        }
    }

    private double generateFallbackValue(String sensorType, Severity severity) {
        double val = 0.0;
        switch (sensorType) {
            case VitalSignType.HEART_RATE:
                if (severity == Severity.NORMAL) val = 60.0 + random.nextDouble() * 40.0;
                else if (severity == Severity.ABNORMAL) val = random.nextBoolean() ? (45.0 + random.nextDouble() * 14.0) : (101.0 + random.nextDouble() * 29.0);
                else val = random.nextBoolean() ? (30.0 + random.nextDouble() * 14.0) : (131.0 + random.nextDouble() * 45.0);
                break;
            case VitalSignType.SPO2:
                if (severity == Severity.NORMAL) val = 95.0 + random.nextDouble() * 5.0;
                else if (severity == Severity.ABNORMAL) val = 90.0 + random.nextDouble() * 4.9;
                else val = 70.0 + random.nextDouble() * 19.9;
                break;
            case VitalSignType.TEMPERATURE:
                if (severity == Severity.NORMAL) val = 36.1 + random.nextDouble() * 1.4;
                else if (severity == Severity.ABNORMAL) val = random.nextBoolean() ? (35.0 + random.nextDouble() * 1.0) : (37.6 + random.nextDouble() * 1.4);
                else val = random.nextBoolean() ? (32.0 + random.nextDouble() * 2.9) : (39.1 + random.nextDouble() * 2.4);
                break;
            case VitalSignType.BLOOD_PRESSURE:
                if (severity == Severity.NORMAL) val = 90.0 + random.nextDouble() * 39.0;
                else if (severity == Severity.ABNORMAL) val = random.nextBoolean() ? (80.0 + random.nextDouble() * 9.9) : (130.0 + random.nextDouble() * 29.9);
                else val = random.nextBoolean() ? (55.0 + random.nextDouble() * 24.9) : (160.0 + random.nextDouble() * 40.0);
                break;
            case VitalSignType.RESPIRATION:
                if (severity == Severity.NORMAL) val = 12.0 + random.nextDouble() * 8.0;
                else if (severity == Severity.ABNORMAL) val = random.nextBoolean() ? (9.0 + random.nextDouble() * 2.9) : (21.0 + random.nextDouble() * 3.9);
                else val = random.nextBoolean() ? (5.0 + random.nextDouble() * 3.0) : (25.0 + random.nextDouble() * 10.0);
                break;
            case VitalSignType.ECG_HRV:
                if (severity == Severity.NORMAL) val = 40.0 + random.nextDouble() * 60.0;
                else if (severity == Severity.ABNORMAL) val = random.nextBoolean() ? (25.0 + random.nextDouble() * 14.0) : (101.0 + random.nextDouble() * 29.0);
                else val = random.nextBoolean() ? (10.0 + random.nextDouble() * 14.0) : (131.0 + random.nextDouble() * 30.0);
                break;
            default:
                val = 100.0;
                break;
        }
        return Math.round(val * 10.0) / 10.0;
    }
}
