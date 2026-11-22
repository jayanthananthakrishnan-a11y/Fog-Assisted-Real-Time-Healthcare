package priority_aware;

import common.ReadingRecord;
import common.Severity;
import common.SimulationConfig;
import common.VitalSignType;
import fog_filtering.FilterAggregatorEngine;
import java.util.*;

/**
 * Priority-Aware Classification & Routing Engine with Cross-Sensor Artifact Suppression.
 * 
 * Rules:
 * 1. CRITICAL: 
 *    - If accompanied by another abnormal/critical vital OR persists for >= 2 consecutive samples:
 *      Trigger immediate Local Emergency Alert to Bedside Actuator (< 10 ms) + Cloud EHR archiving.
 *    - If an isolated single-sample critical spike occurs while all other vitals are normal:
 *      Suppressed as a motion artifact (preventing ICU alarm fatigue) and held for verification.
 * 2. ABNORMAL: Prompt individual expedited dispatch to Cloud without aggregation delay.
 * 3. NORMAL: Evaluated through delta redundancy filter; non-redundant readings are batched in window W.
 */
public class PriorityProcessorEngine {

    public enum PriorityDecision {
        CRITICAL_DUAL_DISPATCH,
        CRITICAL_ARTIFACT_SUPPRESSED,
        ABNORMAL_EXPEDITED,
        NORMAL_FILTERED_DROPPED,
        NORMAL_BUFFERED_IN_WINDOW,
        NORMAL_BATCH_READY
    }

    private final FilterAggregatorEngine normalAggregator;
    private final boolean enableArtifactSuppression;

    // Per-patient recent vital states for cross-sensor correlation
    private final Map<Integer, Map<String, ReadingRecord>> patientRecentVitals = new HashMap<>();
    private final Map<String, Integer> consecutiveCriticalCounts = new HashMap<>();

    public PriorityProcessorEngine() {
        this(SimulationConfig.aggregationWindowSize, SimulationConfig.enableArtifactSuppression);
    }

    public PriorityProcessorEngine(int windowSize) {
        this(windowSize, SimulationConfig.enableArtifactSuppression);
    }

    public PriorityProcessorEngine(int windowSize, boolean enableArtifactSuppression) {
        this.normalAggregator = new FilterAggregatorEngine(windowSize);
        this.enableArtifactSuppression = enableArtifactSuppression;
    }

    public synchronized PriorityDecision classifyAndRoute(ReadingRecord reading,
                                                           List<FilterAggregatorEngine.BatchInfo> readyBatchesOut) {
        Severity severity = reading.getSeverity();
        int patientId = reading.getPatientId();
        String sensorType = reading.getSensorType();
        String sensorKey = patientId + "_" + sensorType;

        // 1. Critical Priority: Emergency Check with Cross-Sensor Artifact Suppression
        if (severity == Severity.CRITICAL) {
            if (enableArtifactSuppression) {
                int prevCount = consecutiveCriticalCounts.getOrDefault(sensorKey, 0);
                int currentConsecutive = prevCount + 1;
                consecutiveCriticalCounts.put(sensorKey, currentConsecutive);

                // Check cross-sensor correlation: Is any OTHER sensor for this patient abnormal or critical?
                boolean crossSensorCorrelation = false;
                Map<String, ReadingRecord> vitals = patientRecentVitals.get(patientId);
                if (vitals != null) {
                    for (Map.Entry<String, ReadingRecord> entry : vitals.entrySet()) {
                        if (!entry.getKey().equals(sensorType)) {
                            Severity otherSev = entry.getValue().getSeverity();
                            if (otherSev == Severity.ABNORMAL || otherSev == Severity.CRITICAL) {
                                crossSensorCorrelation = true;
                                break;
                            }
                        }
                    }
                }

                // If cross-sensor correlation exists OR persisted across 2+ consecutive samples -> True Emergency
                if (crossSensorCorrelation || currentConsecutive >= 2) {
                    patientRecentVitals.computeIfAbsent(patientId, k -> new HashMap<>()).put(sensorType, reading);
                    return PriorityDecision.CRITICAL_DUAL_DISPATCH;
                } else {
                    // Isolated single-sample critical spike with all other vitals normal -> Suppress motion artifact!
                    patientRecentVitals.computeIfAbsent(patientId, k -> new HashMap<>()).put(sensorType, reading);
                    return PriorityDecision.CRITICAL_ARTIFACT_SUPPRESSED;
                }
            } else {
                return PriorityDecision.CRITICAL_DUAL_DISPATCH;
            }
        }

        // Reset consecutive critical counter when reading is not critical
        consecutiveCriticalCounts.put(sensorKey, 0);
        patientRecentVitals.computeIfAbsent(patientId, k -> new HashMap<>()).put(sensorType, reading);

        // 2. Abnormal Priority: Expedited Forwarding without aggregation delay
        if (severity == Severity.ABNORMAL) {
            return PriorityDecision.ABNORMAL_EXPEDITED;
        }

        // 3. Normal Priority: Redundancy Filtering + Window Aggregation
        FilterAggregatorEngine.FilterDecision decision = normalAggregator.evaluateReading(reading, readyBatchesOut);
        if (decision == FilterAggregatorEngine.FilterDecision.FILTERED_DROPPED) {
            return PriorityDecision.NORMAL_FILTERED_DROPPED;
        } else if (decision == FilterAggregatorEngine.FilterDecision.BATCH_READY) {
            return PriorityDecision.NORMAL_BATCH_READY;
        } else {
            return PriorityDecision.NORMAL_BUFFERED_IN_WINDOW;
        }
    }

    public synchronized List<FilterAggregatorEngine.BatchInfo> flushAllBuffers() {
        return normalAggregator.flushAllBuffers();
    }

    public synchronized FilterAggregatorEngine.BatchInfo getBatch(int batchId) {
        return normalAggregator.getBatch(batchId);
    }
}
