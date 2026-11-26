package experiments;

import priority_aware.AdaptiveTcpWindowController;
import priority_aware.TlsOverheadBenchmark;
import priority_aware.PatientReportGenerator;
import priority_aware.PrioritySchedulerBenchmark;
import priority_aware.ArchitectureOptimalityBenchmark;
import multi_ward.ExtremeScalabilityBenchmark;
import multi_ward.FaultToleranceBenchmark;
import multi_ward.HealthcareMultiWardFailover;

/**
 * Master runner for Experiments 5 through 11 and Automated Patient Reports.
 */
public class ComprehensiveExperimentRunner {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println(" Executing Comprehensive Real PhysioNet Simulation Suite (Exp 5 - 11)   ");
        System.out.println("==========================================================================");

        // Exp 6: Adaptive TCP Window AIMD
        System.out.println("\n>>> [1/7] Running Exp 6: Adaptive TCP Window AIMD Controller...");
        AdaptiveTcpWindowController.generateExp6Dataset("results/csv/exp6_adaptive_tcp_window.csv");

        // Exp 7: TLS 1.3 Security Overhead & Realistic SLA Breakdown
        System.out.println("\n>>> [2/7] Running Exp 7: TLS 1.3 Overhead & Realistic SLA Availability...");
        TlsOverheadBenchmark.generateExp7Dataset("results/csv/exp7_tls_overhead.csv");

        // Exp 8: Fog Node Scaling & Extreme Stress
        System.out.println("\n>>> [3/7] Running Exp 8: Fog Gateway Node Scaling (1 to 16 nodes)...");
        ExtremeScalabilityBenchmark.generateExp8Dataset("results/csv/exp8_fog_node_scaling.csv");

        // Exp 9: Fault-Tolerance Strategies
        System.out.println("\n>>> [4/7] Running Exp 9: Fault-Tolerance Strategies Comparison...");
        FaultToleranceBenchmark.generateExp9Dataset("results/csv/exp9_fault_tolerance_strategies.csv");

        // Exp 10: Scheduling Policy Comparison
        System.out.println("\n>>> [5/7] Running Exp 10: Priority Fog Scheduling Policy Comparison...");
        PrioritySchedulerBenchmark.runBenchmark("results/csv/exp10_priority_scheduler_comparison.csv");

        // Exp 11: Multi-Objective Architecture Optimality Matrix
        System.out.println("\n>>> [6/7] Running Exp 11: Multi-Objective Architecture Optimization Matrix...");
        ArchitectureOptimalityBenchmark.runBenchmark("results/csv/exp11_architecture_optimality_matrix.csv");

        // Automated Patient Clinical Reports
        System.out.println("\n>>> Generating Automated Per-Patient Clinical Summary Reports...");
        PatientReportGenerator.exportReports("results/patient_reports", 10);

        // Exp 5: Multi-Ward Failover (CloudSim calls System.exit(0) upon finish, so run this last)
        System.out.println("\n>>> [7/7] Running Exp 5: Multi-Ward Two-Gateway Failover Benchmark...");
        HealthcareMultiWardFailover.runSimulation(true);
    }
}
