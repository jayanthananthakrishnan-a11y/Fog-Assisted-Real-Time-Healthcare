package experiments;

import priority_aware.AdaptiveTcpWindowController;
import priority_aware.TlsOverheadBenchmark;
import priority_aware.PatientReportGenerator;
import multi_ward.ExtremeScalabilityBenchmark;
import multi_ward.FaultToleranceBenchmark;
import multi_ward.HealthcareMultiWardFailover;

/**
 * Master runner for Experiments 5 through 9 and Automated Patient Reports.
 */
public class ComprehensiveExperimentRunner {

    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println(" Executing Comprehensive Professor Review Simulation Suite (Exp 5 - 9) ");
        System.out.println("========================================================================");

        // Exp 6: Adaptive TCP Window AIMD
        System.out.println("\n>>> [1/5] Running Exp 6: Adaptive TCP Window AIMD Controller...");
        AdaptiveTcpWindowController.generateExp6Dataset("results/csv/exp6_adaptive_tcp_window.csv");

        // Exp 7: TLS 1.3 Security Overhead
        System.out.println("\n>>> [2/5] Running Exp 7: TLS 1.3 Overhead & Cryptographic Cost...");
        TlsOverheadBenchmark.generateExp7Dataset("results/csv/exp7_tls_overhead.csv");

        // Exp 8: Fog Node Scaling & Extreme Stress
        System.out.println("\n>>> [3/5] Running Exp 8: Fog Gateway Node Scaling (1 to 16 nodes)...");
        ExtremeScalabilityBenchmark.generateExp8Dataset("results/csv/exp8_fog_node_scaling.csv");

        // Exp 9: Fault-Tolerance Strategies
        System.out.println("\n>>> [4/5] Running Exp 9: Fault-Tolerance Strategies Comparison...");
        FaultToleranceBenchmark.generateExp9Dataset("results/csv/exp9_fault_tolerance_strategies.csv");

        // Automated Patient Clinical Reports
        System.out.println("\n>>> Generating Automated Per-Patient Clinical Summary Reports...");
        PatientReportGenerator.exportReports("results/patient_reports", 10);

        // Exp 5: Multi-Ward Failover (CloudSim calls System.exit(0) upon finish, so run this last)
        System.out.println("\n>>> [5/5] Running Exp 5: Multi-Ward Two-Gateway Failover Benchmark...");
        HealthcareMultiWardFailover.runSimulation(true);
    }
}
