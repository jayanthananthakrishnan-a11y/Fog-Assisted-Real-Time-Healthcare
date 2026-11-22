package common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Thread-safe singleton metrics collector for iFogSim healthcare experiments.
 * Accurately tracks every reading event, calculates latency percentiles,
 * network traffic breakdowns, bandwidth savings vs baseline, and exports reproducible CSV reports.
 */
public class MetricsTracker {

    private static MetricsTracker instance;

    private String architectureName = "Architecture A (Cloud-Only Baseline)";
    private final List<ReadingRecord> allReadings = new ArrayList<>();
    private final Map<Integer, ReadingRecord> readingMap = new HashMap<>();

    // Network traffic accounting (bytes)
    private long totalNetworkTrafficBytes = 0;
    private long cloudBoundTrafficBytes = 0;
    private int cloudBoundTuplesCount = 0;

    // Fog processing counters
    private int filteredCount = 0;
    private int artifactsSuppressedCount = 0;
    private int aggregatedBatchesCount = 0;
    private int criticalBypassedCount = 0;

    private MetricsTracker() {}

    public static synchronized MetricsTracker getInstance() {
        if (instance == null) {
            instance = new MetricsTracker();
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = new MetricsTracker();
    }

    public synchronized void setArchitectureName(String name) {
        this.architectureName = name;
    }

    public synchronized String getArchitectureName() {
        return architectureName;
    }

    public synchronized void recordGeneration(ReadingRecord record) {
        allReadings.add(record);
        readingMap.put(record.getReadingId(), record);
        // Sensor to local gateway hop
        totalNetworkTrafficBytes += record.getTupleBytes();
    }

    public synchronized void recordFiltered(int readingId) {
        ReadingRecord record = readingMap.get(readingId);
        if (record != null) {
            record.setFiltered(true);
            filteredCount++;
        }
    }

    public synchronized void recordArtifactSuppressed(int readingId) {
        ReadingRecord record = readingMap.get(readingId);
        if (record != null) {
            record.setArtifactSuppressed(true);
            artifactsSuppressedCount++;
        }
    }

    public synchronized void recordCloudArrival(int readingId, double cloudArrivalTime) {
        ReadingRecord record = readingMap.get(readingId);
        if (record != null) {
            record.setCloudArrivalTime(cloudArrivalTime);
            // Uplink hop to cloud
            cloudBoundTrafficBytes += record.getTupleBytes();
            totalNetworkTrafficBytes += record.getTupleBytes();
            cloudBoundTuplesCount++;
        }
    }

    public synchronized void recordAggregatedBatch(List<Integer> constituentReadingIds, long batchTupleBytes, int batchId) {
        aggregatedBatchesCount++;
        cloudBoundTrafficBytes += batchTupleBytes;
        totalNetworkTrafficBytes += batchTupleBytes;
        cloudBoundTuplesCount++;

        for (int rId : constituentReadingIds) {
            ReadingRecord r = readingMap.get(rId);
            if (r != null) {
                r.setAggregated(true);
                r.setBatchId(batchId);
            }
        }
    }

    public synchronized void recordBatchDelivery(List<Integer> constituentReadingIds, double completeTime) {
        for (int rId : constituentReadingIds) {
            ReadingRecord r = readingMap.get(rId);
            if (r != null) {
                r.setAnalyticsCompleteTime(completeTime);
            }
        }
        // Downlink result confirmation
        totalNetworkTrafficBytes += (SimulationConfig.ANALYTICS_RESULT_NW_SIZE * 2);
    }

    public synchronized void recordCriticalImmediateDelivery(int readingId, double localAlertTime) {
        ReadingRecord record = readingMap.get(readingId);
        if (record != null) {
            record.setCriticalBypassed(true);
            record.setAnalyticsCompleteTime(localAlertTime);
            criticalBypassedCount++;
            // Local alert downlink hop to actuator
            totalNetworkTrafficBytes += SimulationConfig.ANALYTICS_RESULT_NW_SIZE;
        }
    }

    public synchronized void recordDelivery(int readingId, double completeTime) {
        ReadingRecord record = readingMap.get(readingId);
        if (record != null) {
            if (record.isCriticalBypassed()) {
                // Already delivered via emergency local bypass in ~3ms! Do not overwrite with cloud confirmation!
                return;
            }
            record.setAnalyticsCompleteTime(completeTime);
            // Downlink confirmation to actuator (WAN downlink + local hop)
            totalNetworkTrafficBytes += (SimulationConfig.ANALYTICS_RESULT_NW_SIZE * 2);
        }
    }

    public synchronized ReadingRecord getReading(int readingId) {
        return readingMap.get(readingId);
    }

    // --- Statistics Calculations ---

    public synchronized int getTotalGenerated() {
        return allReadings.size();
    }

    public synchronized int getTotalDelivered() {
        int count = 0;
        for (ReadingRecord r : allReadings) {
            if (r.isDelivered()) count++;
        }
        return count;
    }

    public synchronized int getFilteredCount() {
        return filteredCount;
    }

    public synchronized int getArtifactsSuppressedCount() {
        return artifactsSuppressedCount;
    }

    public synchronized int getAggregatedBatchesCount() {
        return aggregatedBatchesCount;
    }

    public synchronized int getCriticalBypassedCount() {
        return criticalBypassedCount;
    }

    public synchronized int getCloudBoundTuplesCount() {
        return cloudBoundTuplesCount;
    }

    public synchronized int getGeneratedBySeverity(Severity severity) {
        int count = 0;
        for (ReadingRecord r : allReadings) {
            if (r.getSeverity() == severity) count++;
        }
        return count;
    }

    public synchronized int getDeliveredBySeverity(Severity severity) {
        int count = 0;
        for (ReadingRecord r : allReadings) {
            if (r.getSeverity() == severity && r.isDelivered()) count++;
        }
        return count;
    }

    public synchronized double getAverageLatency() {
        double sum = 0.0;
        int count = 0;
        for (ReadingRecord r : allReadings) {
            if (r.isDelivered()) {
                sum += r.getEndToEndLatency();
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    public synchronized double getAverageLatencyBySeverity(Severity severity) {
        double sum = 0.0;
        int count = 0;
        for (ReadingRecord r : allReadings) {
            if (r.getSeverity() == severity && r.isDelivered()) {
                sum += r.getEndToEndLatency();
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    public synchronized double getP95Latency() {
        List<Double> latencies = new ArrayList<>();
        for (ReadingRecord r : allReadings) {
            if (r.isDelivered()) {
                latencies.add(r.getEndToEndLatency());
            }
        }
        if (latencies.isEmpty()) return 0.0;
        Collections.sort(latencies);
        int index = (int) Math.ceil(0.95 * latencies.size()) - 1;
        index = Math.max(0, Math.min(index, latencies.size() - 1));
        return latencies.get(index);
    }

    public synchronized double getCriticalDeliveryRate() {
        int gen = getGeneratedBySeverity(Severity.CRITICAL);
        if (gen == 0) return 100.0;
        int del = getDeliveredBySeverity(Severity.CRITICAL);
        return ((double) del / gen) * 100.0;
    }

    public synchronized long getTotalNetworkTrafficBytes() {
        return totalNetworkTrafficBytes;
    }

    public synchronized long getCloudBoundTrafficBytes() {
        return cloudBoundTrafficBytes;
    }

    public synchronized double getTotalNetworkTrafficKB() {
        return totalNetworkTrafficBytes / 1024.0;
    }

    public synchronized double getCloudBoundTrafficKB() {
        return cloudBoundTrafficBytes / 1024.0;
    }

    public synchronized double getCloudBandwidthSavingPercent() {
        long baseline = (long) SimulationConfig.numPatients * SimulationConfig.SENSORS_PER_PATIENT
                * SimulationConfig.READINGS_PER_SENSOR * SimulationConfig.RAW_TUPLE_NW_SIZE;
        if (baseline == 0) return 0.0;
        double saved = baseline - cloudBoundTrafficBytes;
        return (saved / baseline) * 100.0;
    }

    public synchronized double getTotalBandwidthSavingPercent() {
        long baseline = (long) SimulationConfig.numPatients * SimulationConfig.SENSORS_PER_PATIENT
                * SimulationConfig.READINGS_PER_SENSOR * 1400L;
        if (baseline == 0) return 0.0;
        double saved = baseline - totalNetworkTrafficBytes;
        return (saved / baseline) * 100.0;
    }

    public synchronized int getCriticalSlaDeliveredCount() {
        int count = 0;
        for (ReadingRecord r : allReadings) {
            if (r.getSeverity() == Severity.CRITICAL && r.isDelivered()) {
                if (r.getEndToEndLatency() <= SimulationConfig.CRITICAL_SLA_DEADLINE_MS) {
                    count++;
                }
            }
        }
        return count;
    }

    public synchronized double getCriticalSlaReliabilityPercent() {
        int gen = getGeneratedBySeverity(Severity.CRITICAL);
        if (gen == 0) return 100.0;
        return ((double) getCriticalSlaDeliveredCount() / gen) * 100.0;
    }

    public synchronized String toExperimentCsvRow() {
        return String.format(Locale.US,
                "%s,%d,%d,%d,%d,%d,%d,%d,%.2f,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%.2f,%.2f,%.2f,%d,%.2f",
                architectureName,
                SimulationConfig.numPatients,
                SimulationConfig.numPatients * SimulationConfig.SENSORS_PER_PATIENT,
                getTotalGenerated(),
                filteredCount,
                getTotalDelivered(),
                getGeneratedBySeverity(Severity.CRITICAL),
                getDeliveredBySeverity(Severity.CRITICAL),
                getCriticalDeliveryRate(),
                getAverageLatency(),
                getAverageLatencyBySeverity(Severity.NORMAL),
                getAverageLatencyBySeverity(Severity.ABNORMAL),
                getAverageLatencyBySeverity(Severity.CRITICAL),
                getP95Latency(),
                getCloudBoundTrafficKB(),
                getTotalNetworkTrafficKB(),
                getCloudBandwidthSavingPercent(),
                getTotalBandwidthSavingPercent(),
                getCriticalSlaDeliveredCount(),
                getCriticalSlaReliabilityPercent()
        );
    }

    // --- Output & CSV Exports ---

    public synchronized void printSummary() {
        System.out.println("\n========================================================================");
        System.out.printf("                 %s RESULTS           \n", architectureName.toUpperCase());
        System.out.println("========================================================================");
        System.out.printf(Locale.US, "Simulated Patients              : %d\n", SimulationConfig.numPatients);
        System.out.printf(Locale.US, "Sensors Monitored               : %d (%d per patient)\n",
                SimulationConfig.numPatients * SimulationConfig.SENSORS_PER_PATIENT, SimulationConfig.SENSORS_PER_PATIENT);
        System.out.printf(Locale.US, "Total Readings Generated        : %d\n", getTotalGenerated());
        System.out.printf(Locale.US, "  - NORMAL Readings             : %d (%.1f%%)\n",
                getGeneratedBySeverity(Severity.NORMAL), (getGeneratedBySeverity(Severity.NORMAL) * 100.0 / Math.max(1, getTotalGenerated())));
        System.out.printf(Locale.US, "  - ABNORMAL Readings           : %d (%.1f%%)\n",
                getGeneratedBySeverity(Severity.ABNORMAL), (getGeneratedBySeverity(Severity.ABNORMAL) * 100.0 / Math.max(1, getTotalGenerated())));
        System.out.printf(Locale.US, "  - CRITICAL Readings           : %d (%.1f%%)\n",
                getGeneratedBySeverity(Severity.CRITICAL), (getGeneratedBySeverity(Severity.CRITICAL) * 100.0 / Math.max(1, getTotalGenerated())));
        System.out.println("------------------------------------------------------------------------");
        System.out.printf(Locale.US, "Filtered (Dropped at Fog)       : %d (%.2f%% of total)\n",
                filteredCount, (filteredCount * 100.0 / Math.max(1, getTotalGenerated())));
        System.out.printf(Locale.US, "Artifacts Suppressed (False Alarms): %d (%.2f%% of total)\n",
                artifactsSuppressedCount, (artifactsSuppressedCount * 100.0 / Math.max(1, getTotalGenerated())));
        System.out.printf(Locale.US, "Aggregated Batches Emitted      : %d (Window Size W = %d)\n",
                aggregatedBatchesCount, SimulationConfig.aggregationWindowSize);
        System.out.printf(Locale.US, "Critical Emergency Bypasses     : %d\n", criticalBypassedCount);
        System.out.printf(Locale.US, "Cloud-Bound Tuples Sent         : %d\n", cloudBoundTuplesCount);
        System.out.printf(Locale.US, "Total Readings Delivered        : %d (Accounting: Filtered=%d + Suppressed=%d + Delivered=%d = %d/%d)\n",
                getTotalDelivered(), filteredCount, artifactsSuppressedCount, getTotalDelivered(),
                (filteredCount + artifactsSuppressedCount + getTotalDelivered()), getTotalGenerated());
        System.out.printf(Locale.US, "  - Critical Delivered          : %d / %d (%.2f%%)\n",
                getDeliveredBySeverity(Severity.CRITICAL), getGeneratedBySeverity(Severity.CRITICAL), getCriticalDeliveryRate());
        System.out.println("------------------------------------------------------------------------");
        System.out.printf(Locale.US, "Average End-to-End Latency      : %.3f ms\n", getAverageLatency());
        System.out.printf(Locale.US, "  - NORMAL Reading Latency      : %.3f ms\n", getAverageLatencyBySeverity(Severity.NORMAL));
        System.out.printf(Locale.US, "  - ABNORMAL Reading Latency    : %.3f ms\n", getAverageLatencyBySeverity(Severity.ABNORMAL));
        System.out.printf(Locale.US, "  - CRITICAL Reading Latency    : %.3f ms\n", getAverageLatencyBySeverity(Severity.CRITICAL));
        System.out.printf(Locale.US, "P95 End-to-End Latency          : %.3f ms\n", getP95Latency());
        System.out.println("------------------------------------------------------------------------");
        System.out.printf(Locale.US, "Total Cloud-Bound Traffic       : %d Bytes (%.2f KB)\n",
                getCloudBoundTrafficBytes(), getCloudBoundTrafficKB());
        System.out.printf(Locale.US, "Total Network Traffic           : %d Bytes (%.2f KB)\n",
                getTotalNetworkTrafficBytes(), getTotalNetworkTrafficKB());
        System.out.printf(Locale.US, "Cloud Bandwidth Saving vs Arch A: %.2f%%\n", getCloudBandwidthSavingPercent());
        System.out.printf(Locale.US, "Total Bandwidth Saving vs Arch A: %.2f%%\n", getTotalBandwidthSavingPercent());
        System.out.println("========================================================================\n");
    }

    public synchronized void exportToCsv() {
        exportToCsv(SimulationConfig.READINGS_CSV_PATH, SimulationConfig.SUMMARY_CSV_PATH);
    }

    public synchronized void exportToCsv(String readingsPath, String summaryPath) {
        // Export raw reading logs
        try {
            File rFile = new File(readingsPath);
            if (rFile.getParentFile() != null) rFile.getParentFile().mkdirs();
            try (PrintWriter pw = new PrintWriter(new FileWriter(rFile))) {
                pw.println(ReadingRecord.getCsvHeader());
                for (ReadingRecord r : allReadings) {
                    pw.println(r.toCsvRow());
                }
            }
            System.out.println("[MetricsTracker] Exported raw readings log to: " + readingsPath);
        } catch (IOException e) {
            System.err.println("[MetricsTracker] Error writing readings CSV: " + e.getMessage());
        }

        // Export summary metrics
        try {
            File sFile = new File(summaryPath);
            if (sFile.getParentFile() != null) sFile.getParentFile().mkdirs();
            try (PrintWriter pw = new PrintWriter(new FileWriter(sFile))) {
                pw.println("metric,value,unit");
                pw.printf(Locale.US, "architecture,%s,string\n", architectureName);
                pw.printf(Locale.US, "num_patients,%d,count\n", SimulationConfig.numPatients);
                pw.printf(Locale.US, "total_sensors,%d,count\n", SimulationConfig.numPatients * SimulationConfig.SENSORS_PER_PATIENT);
                pw.printf(Locale.US, "total_generated,%d,count\n", getTotalGenerated());
                pw.printf(Locale.US, "total_delivered,%d,count\n", getTotalDelivered());
                pw.printf(Locale.US, "filtered_at_fog,%d,count\n", filteredCount);
                pw.printf(Locale.US, "artifacts_suppressed,%d,count\n", artifactsSuppressedCount);
                pw.printf(Locale.US, "aggregated_batches,%d,count\n", aggregatedBatchesCount);
                pw.printf(Locale.US, "critical_bypassed,%d,count\n", criticalBypassedCount);
                pw.printf(Locale.US, "cloud_bound_tuples,%d,count\n", cloudBoundTuplesCount);
                pw.printf(Locale.US, "normal_generated,%d,count\n", getGeneratedBySeverity(Severity.NORMAL));
                pw.printf(Locale.US, "abnormal_generated,%d,count\n", getGeneratedBySeverity(Severity.ABNORMAL));
                pw.printf(Locale.US, "critical_generated,%d,count\n", getGeneratedBySeverity(Severity.CRITICAL));
                pw.printf(Locale.US, "critical_delivered,%d,count\n", getDeliveredBySeverity(Severity.CRITICAL));
                pw.printf(Locale.US, "critical_delivery_rate,%.2f,percent\n", getCriticalDeliveryRate());
                pw.printf(Locale.US, "critical_sla_delivered,%d,count\n", getCriticalSlaDeliveredCount());
                pw.printf(Locale.US, "critical_sla_reliability_pct,%.2f,percent\n", getCriticalSlaReliabilityPercent());
                pw.printf(Locale.US, "avg_latency_ms,%.3f,ms\n", getAverageLatency());
                pw.printf(Locale.US, "avg_normal_latency_ms,%.3f,ms\n", getAverageLatencyBySeverity(Severity.NORMAL));
                pw.printf(Locale.US, "avg_abnormal_latency_ms,%.3f,ms\n", getAverageLatencyBySeverity(Severity.ABNORMAL));
                pw.printf(Locale.US, "avg_critical_latency_ms,%.3f,ms\n", getAverageLatencyBySeverity(Severity.CRITICAL));
                pw.printf(Locale.US, "p95_latency_ms,%.3f,ms\n", getP95Latency());
                pw.printf(Locale.US, "cloud_bound_traffic_bytes,%d,bytes\n", getCloudBoundTrafficBytes());
                pw.printf(Locale.US, "cloud_bound_traffic_kb,%.2f,KB\n", getCloudBoundTrafficKB());
                pw.printf(Locale.US, "total_network_traffic_bytes,%d,bytes\n", getTotalNetworkTrafficBytes());
                pw.printf(Locale.US, "total_network_traffic_kb,%.2f,KB\n", getTotalNetworkTrafficKB());
                pw.printf(Locale.US, "cloud_bandwidth_saving_percent,%.2f,percent\n", getCloudBandwidthSavingPercent());
                pw.printf(Locale.US, "total_bandwidth_saving_percent,%.2f,percent\n", getTotalBandwidthSavingPercent());
            }
            System.out.println("[MetricsTracker] Exported summary metrics to: " + summaryPath);
        } catch (IOException e) {
            System.err.println("[MetricsTracker] Error writing summary CSV: " + e.getMessage());
        }
    }
}
