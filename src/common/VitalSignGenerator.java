package common;

import java.util.Random;

/**
 * Deterministic generator for synthetic physiological vital signs.
 * Uses a fixed random seed (seed = 42L) to produce consistent, reproducible
 * workloads across all architectures.
 */
public class VitalSignGenerator {

    private final Random random;

    public VitalSignGenerator() {
        this.random = new Random(SimulationConfig.RANDOM_SEED);
    }

    public VitalSignGenerator(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Generates a reading for a specific patient and sensor type.
     */
    public ReadingRecord generateReading(int readingId, int patientId, int gatewayId, String sensorType, double generateTime) {
        double r = random.nextDouble();
        Severity severity;
        if (r < SimulationConfig.NORMAL_PROBABILITY) {
            severity = Severity.NORMAL;
        } else if (r < SimulationConfig.NORMAL_PROBABILITY + SimulationConfig.ABNORMAL_PROBABILITY) {
            severity = Severity.ABNORMAL;
        } else {
            severity = Severity.CRITICAL;
        }

        double value = generateValue(sensorType, severity);
        return new ReadingRecord(readingId, patientId, gatewayId, sensorType, value, severity, generateTime, SimulationConfig.RAW_TUPLE_NW_SIZE);
    }

    private double generateValue(String sensorType, Severity severity) {
        double val = 0.0;
        switch (sensorType) {
            case VitalSignType.HEART_RATE:
                if (severity == Severity.NORMAL) {
                    val = 60.0 + random.nextDouble() * 40.0; // 60.0 - 100.0 BPM
                } else if (severity == Severity.ABNORMAL) {
                    if (random.nextBoolean()) {
                        val = 45.0 + random.nextDouble() * 14.0; // 45.0 - 59.0 BPM (Bradycardia)
                    } else {
                        val = 101.0 + random.nextDouble() * 29.0; // 101.0 - 130.0 BPM (Mild Tachycardia)
                    }
                } else { // CRITICAL
                    if (random.nextBoolean()) {
                        val = 30.0 + random.nextDouble() * 14.0; // 30.0 - 44.0 BPM (Severe Bradycardia)
                    } else {
                        val = 131.0 + random.nextDouble() * 45.0; // 131.0 - 176.0 BPM (Severe Tachycardia)
                    }
                }
                break;

            case VitalSignType.SPO2:
                if (severity == Severity.NORMAL) {
                    val = 95.0 + random.nextDouble() * 5.0; // 95.0% - 100.0%
                } else if (severity == Severity.ABNORMAL) {
                    val = 90.0 + random.nextDouble() * 4.9; // 90.0% - 94.9% (Mild Hypoxia)
                } else { // CRITICAL
                    val = 70.0 + random.nextDouble() * 19.9; // 70.0% - 89.9% (Severe Hypoxia)
                }
                break;

            case VitalSignType.TEMPERATURE:
                if (severity == Severity.NORMAL) {
                    val = 36.1 + random.nextDouble() * 1.4; // 36.1 - 37.5 °C
                } else if (severity == Severity.ABNORMAL) {
                    if (random.nextBoolean()) {
                        val = 35.0 + random.nextDouble() * 1.0; // 35.0 - 36.0 °C (Mild Hypothermia)
                    } else {
                        val = 37.6 + random.nextDouble() * 1.4; // 37.6 - 39.0 °C (Fever)
                    }
                } else { // CRITICAL
                    if (random.nextBoolean()) {
                        val = 32.0 + random.nextDouble() * 2.9; // 32.0 - 34.9 °C (Severe Hypothermia)
                    } else {
                        val = 39.1 + random.nextDouble() * 2.4; // 39.1 - 41.5 °C (Hyperpyrexia)
                    }
                }
                break;

            case VitalSignType.BLOOD_PRESSURE:
                if (severity == Severity.NORMAL) {
                    val = 90.0 + random.nextDouble() * 39.0; // 90 - 129 mmHg
                } else if (severity == Severity.ABNORMAL) {
                    if (random.nextBoolean()) {
                        val = 80.0 + random.nextDouble() * 9.9; // 80 - 89.9 mmHg (Borderline Hypotension)
                    } else {
                        val = 130.0 + random.nextDouble() * 29.9; // 130 - 159.9 mmHg (Stage 1/2 Hypertension)
                    }
                } else { // CRITICAL
                    if (random.nextBoolean()) {
                        val = 55.0 + random.nextDouble() * 24.9; // 55 - 79.9 mmHg (Severe Hypotension / Shock)
                    } else {
                        val = 160.0 + random.nextDouble() * 40.0; // 160 - 200 mmHg (Hypertensive Crisis)
                    }
                }
                break;

            default:
                val = 100.0;
                break;
        }
        return Math.round(val * 10.0) / 10.0; // Round to 1 decimal place
    }
}
