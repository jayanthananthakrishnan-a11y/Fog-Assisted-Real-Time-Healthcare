package priority_aware;

import common.Severity;
import common.SimulationConfig;
import java.util.PriorityQueue;
import java.util.Comparator;

/**
 * Greedy Earliest-Deadline-First (EDF) + Preemptive Strict Priority Scheduler.
 * 
 * Formal Deadlines:
 * - Tier 1: CRITICAL (tau_crit = 500 ms) - Preempts all lower tiers
 * - Tier 2: ABNORMAL (tau_abn = 2,000 ms) - High priority expedited
 * - Tier 3: NORMAL   (tau_norm = 10,000 ms) - Low priority batched
 * 
 * Within each priority tier, ties are broken greedily by earliest absolute deadline:
 *   t_deadline = t_generation + tau_tier
 */
public class GreedyPriorityScheduler {

    public static class ScheduledTask {
        public final int taskId;
        public final int patientId;
        public final Severity severity;
        public final double generateTime;
        public final double deadlineTime;
        public final long cpuLengthMips;
        public double startTime = -1;
        public double finishTime = -1;

        public ScheduledTask(int taskId, int patientId, Severity severity, double generateTime, long cpuLengthMips) {
            this.taskId = taskId;
            this.patientId = patientId;
            this.severity = severity;
            this.generateTime = generateTime;
            this.cpuLengthMips = cpuLengthMips;

            double deadlineDelta = switch (severity) {
                case CRITICAL -> SimulationConfig.SLA_CRITICAL_MS;
                case ABNORMAL -> SimulationConfig.SLA_ABNORMAL_MS;
                case NORMAL -> SimulationConfig.SLA_NORMAL_MS;
            };
            this.deadlineTime = generateTime + deadlineDelta;
        }

        public int getPriorityTier() {
            return switch (severity) {
                case CRITICAL -> 1;
                case ABNORMAL -> 2;
                case NORMAL -> 3;
            };
        }
    }

    private final PriorityQueue<ScheduledTask> greedyQueue;
    private double currentClock = 0.0;
    private final double gatewayMips;

    public GreedyPriorityScheduler(double gatewayMips) {
        this.gatewayMips = gatewayMips;
        // Strict Priority comparator: Tier first, then earliest deadline
        this.greedyQueue = new PriorityQueue<>(Comparator
                .comparingInt(ScheduledTask::getPriorityTier)
                .thenComparingDouble(t -> t.deadlineTime)
        );
    }

    public synchronized void submitTask(ScheduledTask task) {
        greedyQueue.add(task);
    }

    public synchronized ScheduledTask processNext() {
        if (greedyQueue.isEmpty()) return null;

        ScheduledTask task = greedyQueue.poll();
        task.startTime = Math.max(currentClock, task.generateTime);
        double executionDuration = (task.cpuLengthMips / gatewayMips) * 1000.0; // ms
        task.finishTime = task.startTime + executionDuration;
        currentClock = task.finishTime;

        return task;
    }

    public synchronized boolean hasPendingTasks() {
        return !greedyQueue.isEmpty();
    }

    public synchronized int getQueueSize() {
        return greedyQueue.size();
    }
}
