package common;

import java.util.Locale;

/**
 * Encapsulates an individual physiological sensor reading event,
 * tracking its complete lifecycle from generation to cloud delivery
 * or edge filtering.
 */
public class ReadingRecord {

    private final int readingId;
    private final int patientId;
    private final int patientGatewayId;
    private final String sensorType;
    private final double value;
    private final Severity severity;
    private final double generateTime;
    private double cloudArrivalTime;
    private double analyticsCompleteTime;
    private double endToEndLatency;
    private final long tupleBytes;
    private boolean delivered;

    // Fog Processing Lifecycle Flags
    private boolean filtered;
    private boolean aggregated;
    private boolean criticalBypassed;
    private boolean artifactSuppressed;
    private int batchId;

    public ReadingRecord(int readingId, int patientId, int patientGatewayId, String sensorType,
            double value, Severity severity, double generateTime, long tupleBytes) {
        this.readingId = readingId;
        this.patientId = patientId;
        this.patientGatewayId = patientGatewayId;
        this.sensorType = sensorType;
        this.value = value;
        this.severity = severity;
        this.generateTime = generateTime;
        this.tupleBytes = tupleBytes;
        this.cloudArrivalTime = -1.0;
        this.analyticsCompleteTime = -1.0;
        this.endToEndLatency = -1.0;
        this.delivered = false;
        this.filtered = false;
        this.aggregated = false;
        this.criticalBypassed = false;
        this.artifactSuppressed = false;
        this.batchId = -1;
    }

    public int getReadingId() {
        return readingId;
    }

    public int getPatientId() {
        return patientId;
    }

    public int getPatientGatewayId() {
        return patientGatewayId;
    }

    public String getSensorType() {
        return sensorType;
    }

    public double getValue() {
        return value;
    }

    public Severity getSeverity() {
        return severity;
    }

    public double getGenerateTime() {
        return generateTime;
    }

    public double getCloudArrivalTime() {
        return cloudArrivalTime;
    }

    public void setCloudArrivalTime(double cloudArrivalTime) {
        this.cloudArrivalTime = cloudArrivalTime;
    }

    public double getAnalyticsCompleteTime() {
        return analyticsCompleteTime;
    }

    public void setAnalyticsCompleteTime(double analyticsCompleteTime) {
        this.analyticsCompleteTime = analyticsCompleteTime;
        this.endToEndLatency = analyticsCompleteTime - generateTime;
        this.delivered = true;
    }

    public double getEndToEndLatency() {
        return endToEndLatency;
    }

    public long getTupleBytes() {
        return tupleBytes;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public boolean isFiltered() {
        return filtered;
    }

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    public boolean isAggregated() {
        return aggregated;
    }

    public void setAggregated(boolean aggregated) {
        this.aggregated = aggregated;
    }

    public boolean isCriticalBypassed() {
        return criticalBypassed;
    }

    public void setCriticalBypassed(boolean criticalBypassed) {
        this.criticalBypassed = criticalBypassed;
    }

    public boolean isArtifactSuppressed() {
        return artifactSuppressed;
    }

    public void setArtifactSuppressed(boolean artifactSuppressed) {
        this.artifactSuppressed = artifactSuppressed;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public static String getCsvHeader() {
        return "readingId,patientId,gatewayId,sensorType,value,severity,generateTime,cloudArrivalTime,analyticsCompleteTime,endToEndLatency,tupleBytes,delivered,filtered,aggregated,criticalBypassed,artifactSuppressed";
    }

    public String toCsvRow() {
        return String.format(Locale.US, "%d,%d,%d,%s,%.2f,%s,%.3f,%.3f,%.3f,%.3f,%d,%b,%b,%b,%b,%b",
                readingId,
                patientId,
                patientGatewayId,
                sensorType,
                value,
                severity.name(),
                generateTime,
                cloudArrivalTime,
                analyticsCompleteTime,
                endToEndLatency,
                tupleBytes,
                delivered,
                filtered,
                aggregated,
                criticalBypassed,
                artifactSuppressed);
    }
}
