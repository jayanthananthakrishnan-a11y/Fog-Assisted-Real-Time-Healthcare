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

    // Bi-Directional Hop-by-Hop Latency Breakdowns (ms)
    private double edgeToFogLatency = 0.0;
    private double fogToCloudLatency = 0.0;
    private double cloudToFogLatency = 0.0;
    private double fogToEdgeLatency = 0.0;

    // Per-Tier SLA Violation Attribution
    private String slaViolationReason = "NONE"; // NONE, Window_Buffer_Capacity, Fog_CPU_Queueing, WAN_Transit_Delay

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

    public double getEdgeToFogLatency() {
        return edgeToFogLatency;
    }

    public void setEdgeToFogLatency(double edgeToFogLatency) {
        this.edgeToFogLatency = edgeToFogLatency;
    }

    public double getFogToCloudLatency() {
        return fogToCloudLatency;
    }

    public void setFogToCloudLatency(double fogToCloudLatency) {
        this.fogToCloudLatency = fogToCloudLatency;
    }

    public double getCloudToFogLatency() {
        return cloudToFogLatency;
    }

    public void setCloudToFogLatency(double cloudToFogLatency) {
        this.cloudToFogLatency = cloudToFogLatency;
    }

    public double getFogToEdgeLatency() {
        return fogToEdgeLatency;
    }

    public void setFogToEdgeLatency(double fogToEdgeLatency) {
        this.fogToEdgeLatency = fogToEdgeLatency;
    }

    public String getSlaViolationReason() {
        return slaViolationReason;
    }

    public void setSlaViolationReason(String slaViolationReason) {
        this.slaViolationReason = slaViolationReason;
    }

    public static String getCsvHeader() {
        return "readingId,patientId,gatewayId,sensorType,value,severity,generateTime,cloudArrivalTime,analyticsCompleteTime,endToEndLatency,tupleBytes,delivered,filtered,aggregated,criticalBypassed,artifactSuppressed,edgeToFogMs,fogToCloudMs,cloudToFogMs,fogToEdgeMs,slaViolationReason";
    }

    public String toCsvRow() {
        return String.format(Locale.US, "%d,%d,%d,%s,%.2f,%s,%.3f,%.3f,%.3f,%.3f,%d,%b,%b,%b,%b,%b,%.3f,%.3f,%.3f,%.3f,%s",
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
                artifactSuppressed,
                edgeToFogLatency,
                fogToCloudLatency,
                cloudToFogLatency,
                fogToEdgeLatency,
                slaViolationReason);
    }
}
