package fog_filtering;

import common.ReadingRecord;
import common.Severity;
import common.SimulationConfig;
import common.VitalSignType;
import java.util.*;

/**
 * Stateful edge engine implementing:
 * 1. Delta-based redundancy filtering for NORMAL readings.
 * 2. Sliding window aggregation (size W) for non-redundant readings.
 */
public class FilterAggregatorEngine {

    public enum FilterDecision {
        FILTERED_DROPPED,
        BUFFERED_IN_WINDOW,
        BATCH_READY
    }

    public static class BatchInfo {
        public final int batchId;
        public final String streamKey;
        public final List<Integer> constituentReadingIds;
        public final double avgValue;
        public final double minValue;
        public final double maxValue;

        public BatchInfo(int batchId, String streamKey, List<Integer> constituentReadingIds,
                         double avgValue, double minValue, double maxValue) {
            this.batchId = batchId;
            this.streamKey = streamKey;
            this.constituentReadingIds = constituentReadingIds;
            this.avgValue = avgValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }
    }

    private static int nextBatchId = 1;
    private final int windowSize;

    // Last forwarded reading value per patient and sensor: "patientId_sensorType" -> value
    private final Map<String, Double> lastForwardedValues = new HashMap<>();

    // Buffers per patient and sensor: "patientId_sensorType" -> list of ReadingRecords
    private final Map<String, List<ReadingRecord>> windowBuffers = new HashMap<>();

    // Map batchId -> BatchInfo for completion tracking
    private final Map<Integer, BatchInfo> activeBatches = new HashMap<>();

    public FilterAggregatorEngine() {
        this.windowSize = SimulationConfig.aggregationWindowSize;
    }

    public FilterAggregatorEngine(int windowSize) {
        this.windowSize = windowSize;
    }

    /**
     * Evaluates an incoming reading:
     * - Returns FILTERED_DROPPED if redundant NORMAL reading.
     * - Buffers the reading into the window.
     * - Returns BATCH_READY if window size W is reached.
     */
    public synchronized FilterDecision evaluateReading(ReadingRecord reading, List<BatchInfo> readyBatchesOut) {
        String key = reading.getPatientId() + "_" + reading.getSensorType();

        // Step 1: Stateful Redundancy Filtering (only applied to NORMAL readings)
        if (reading.getSeverity() == Severity.NORMAL) {
            if (lastForwardedValues.containsKey(key)) {
                double lastVal = lastForwardedValues.get(key);
                double delta = Math.abs(reading.getValue() - lastVal);
                double threshold = getDeltaThreshold(reading.getSensorType());

                if (delta < threshold) {
                    // Dropped as redundant at the fog gateway
                    return FilterDecision.FILTERED_DROPPED;
                }
            }
        }

        // Update last forwarded reference value
        lastForwardedValues.put(key, reading.getValue());

        // Step 2: Window Aggregation
        if (!windowBuffers.containsKey(key)) {
            windowBuffers.put(key, new ArrayList<>());
        }
        List<ReadingRecord> buffer = windowBuffers.get(key);
        buffer.add(reading);

        if (buffer.size() >= windowSize) {
            BatchInfo batch = createBatch(key, buffer);
            buffer.clear();
            readyBatchesOut.add(batch);
            activeBatches.put(batch.batchId, batch);
            return FilterDecision.BATCH_READY;
        }

        return FilterDecision.BUFFERED_IN_WINDOW;
    }

    /**
     * Flushes all partially-filled aggregation buffers at simulation drain time.
     */
    public synchronized List<BatchInfo> flushAllBuffers() {
        List<BatchInfo> flushedBatches = new ArrayList<>();
        for (Map.Entry<String, List<ReadingRecord>> entry : windowBuffers.entrySet()) {
            List<ReadingRecord> buffer = entry.getValue();
            if (!buffer.isEmpty()) {
                BatchInfo batch = createBatch(entry.getKey(), buffer);
                buffer.clear();
                flushedBatches.add(batch);
                activeBatches.put(batch.batchId, batch);
            }
        }
        return flushedBatches;
    }

    private BatchInfo createBatch(String key, List<ReadingRecord> buffer) {
        List<Integer> ids = new ArrayList<>();
        double sum = 0.0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (ReadingRecord r : buffer) {
            ids.add(r.getReadingId());
            double val = r.getValue();
            sum += val;
            if (val < min) min = val;
            if (val > max) max = val;
        }
        double avg = sum / buffer.size();
        return new BatchInfo(nextBatchId++, key, ids, avg, min, max);
    }

    public synchronized BatchInfo getBatch(int batchId) {
        return activeBatches.get(batchId);
    }

    private double getDeltaThreshold(String sensorType) {
        switch (sensorType) {
            case VitalSignType.HEART_RATE:
                return SimulationConfig.HR_DELTA_THRESHOLD;
            case VitalSignType.SPO2:
                return SimulationConfig.SPO2_DELTA_THRESHOLD;
            case VitalSignType.TEMPERATURE:
                return SimulationConfig.TEMP_DELTA_THRESHOLD;
            case VitalSignType.BLOOD_PRESSURE:
                return SimulationConfig.BP_DELTA_THRESHOLD;
            default:
                return 1.0;
        }
    }
}
