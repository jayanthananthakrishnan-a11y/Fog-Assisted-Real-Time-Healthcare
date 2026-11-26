# Fog-Assisted Real-Time Healthcare Monitoring System (iFogSim2)

**Topic 19 — Cloud & Fog Computing Research Project**  
**Repository:** [https://github.com/jayanthananthakrishnan-a11y/Fog-Assisted-Real-Time-Healthcare.git](https://github.com/jayanthananthakrishnan-a11y/Fog-Assisted-Real-Time-Healthcare.git)  
**Authors:** Jayanth A, Mirthun V, Vinod K  
**Simulator Platform:** iFogSim2 / CloudSim 7 / Java 21+  

---

## 1. Quick Start (Fresh Clone on Windows)

Clone the repository and run the end-to-end simulation or launch the interactive 3D studio in three steps:

```powershell
# Clone repository
git clone https://github.com/jayanthananthakrishnan-a11y/Fog-Assisted-Real-Time-Healthcare.git
cd Fog-Assisted-Real-Time-Healthcare

# 1. Compile iFogSim2 simulator core and dependencies (one-time setup)
powershell -ExecutionPolicy Bypass -File .\simulator\build.ps1

# 2. Run all 5 architectures (A, B, C, D, E) sequentially
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Arch ALL -NumPatients 10 -WindowSize 5

# 3. Launch the interactive 3D Hospital Ward Visualizer (Web UI at http://localhost:8000)
powershell -ExecutionPolicy Bypass -File .\dashboard\start_dashboard.ps1
```

Once started, open **[http://localhost:8000](http://localhost:8000)** in any modern web browser to interact with the 3D Ward Visualizer and layman architecture guides.

---

## 2. Project Overview

Continuous physiological telemetry streams (Heart Rate/ECG, Pulse Oximetry/SpO2, Body Temperature, Blood Pressure) cannot tolerate high WAN propagation delays (100–200 ms) and create network congestion if streamed uncompressed to remote cloud datacenters.

This research project designs, implements, and benchmarks a multi-tier fog-cloud healthcare monitoring architecture evaluated across five comparative models:

### Evaluated Architectures

| Architecture | Model Description | Critical Alert Latency | Cloud Bandwidth Saving | Realistic SLA Reliability (&le; 500 ms) | Composite Optimality Rank | Key Feature / Tradeoff |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| **Arch A** | **Cloud-Only Baseline** | $207.71\text{ ms}$ | $0.00\%$ (Baseline) | $99.30\%$ (Nominal WAN) | $54.11 / 100$ | Raw IoT streaming; high WAN traffic ($1,953\text{ KB}$) |
| **Arch B** | **Fog Forwarding** | $211.43\text{ ms}$ | $0.00\%$ | $99.20\%$ | $52.86 / 100$ | $+3.72\text{ ms}$ gateway inspection tax with zero data reduction |
| **Arch C** | **Naive Fog Filtering & Aggregation** | $2,293.52\text{ ms}$ | **$77.22\%$** | **$27.88\%$ (72.1% Failure)** | $37.69 / 100$ (Worst) | **Aggregation Window Trap:** Normal buffering delays life-critical alarms |
| **Arch D** | **Priority-Aware Fog Computing** | **$7.13\text{ ms}$** | **$66.62\%$** | **$99.72\%$ (Immune to WAN)** | $90.63 / 100$ | **Dual-Path Routing:** Emergency local bypass + 91.9% motion artifacts suppressed |
| **Arch E** | **Multi-Ward Two-Gateway Failover** | **$7.31\text{ ms}$** (Norm) / **$8.13\text{ ms}$** (Outage) | **$79.64\%$** | **$99.88\%$ (99.96% Avail)** | **$98.34 / 100$ (Rank #1)** | **Two-Ward Mesh:** Seamless $1.0\text{ ms}$ LAN failover during gateway reboots |

---

## 3. Key Research Contributions

1. **Genuine PhysioNet Hospital ICU Dataset Ingestion:** 100% of telemetry is driven by open-access adult ICU patients from **PhysioNet BIDMC PPG and Respiration** (53 patients, 25,493 continuous 1.0 Hz tuples) and **PhysioNet MIMIC-III Clinical Database Demo** (real Core Temperature and Blood Pressure charts). Verified via Pearson/Spearman correlation matrices and autocorrelation decay analysis.
2. **Empirical Sensor DAG & Heterogeneous Sliding Windows:** Models tightly coupled hemodynamics (HR, BP, SpO2, HRV, $CV \approx 12.33\%$) in tight windows ($W \in [2, 5]$) and slow thermal dynamics (Temp, Resp, $CV \approx 2.35\%$) in wide windows ($W \in [10, 20]$), resolving the latency cliff.
3. **Preemptive Priority + Greedy EDF Scheduling (Exp 10):** Proves superiority over FIFO, Strict Static Priority (which starves 428 normal readings), and WFQ (which inflates critical p99 to 84.10 ms), achieving **8.45 ms p99 tail latency** and zero starvation.
4. **Dual-Path Priority Bypass (Architecture D & E):** Separates clinical traffic into high-priority emergency alerts and non-critical routine vitals. Critical telemetry bypasses sliding-window aggregation buffers, routing over local hospital LAN in **$7.13\text{ ms}$** (a **96.6% latency reduction** compared to the $207.71\text{ ms}$ cloud baseline).
5. **Cross-Sensor Motion Artifact Suppression:** Mitigates ICU alarm fatigue by cross-verifying isolated single-sample critical spikes against co-vitals, suppressing **91.9% of spurious false alarms** while retaining 100% sensitivity for true emergencies.
6. **Multi-Ward Two-Gateway Load Balancing & Failover (Architecture E / Exp 5):** Interconnects adjacent ICU wards via a $1.0\text{ ms}$ inter-ward LAN. Under a 20-second gateway power outage, telemetry automatically routes to the peer gateway, preserving sub-$10\text{ ms}$ emergency response and 99.88% SLA compliance.
7. **Empirical TLS 1.3 Security & 4-Leg Latency Attribution (Exp 7):** Profiles +37 byte AES-GCM record framing, +15 MIPS crypto CPU cost, +2.12 ms LAN session vs +102.4 ms WAN handshake penalty, and attributes residual 0.12% SLA violations.
8. **Multi-Objective Architecture Optimality Matrix (Exp 11):** Formulates a normalized Composite Utility Score across Latency, Bandwidth, SLA Availability, Energy, and Fault Recovery, proving that Architecture E is mathematically optimal ($98.34 / 100$).
9. **Interactive 3D Hospital Ward Studio:** A lightweight Three.js and Python web dashboard (`http://localhost:8000`) featuring a 3D floorplan, buffer tracks, live PhysioNet telemetry streaming, and dedicated deep-dive tabs for Architectures A–E, Clinical Reports, and Systems Benchmarks.

---

## 4. Repository Structure

```text
Fog-Assisted-Real-Time-Healthcare/
│
├── README.md                                # Project overview, quick start, and architecture summary
├── project_guidelines.txt                   # Official university project guidelines (Topic 19)
├── setup_workspace.ps1                     # Environment setup script
├── build_and_run.ps1                       # Universal build and simulation runner (Arch A, B, C, D, E, ALL)
├── PHASE_1_SETUP_REPORT.md                 # Setup, verification, and iFogSim2 compilation report
├── PHASE_2_BASELINE_REPORT.md              # Architecture A baseline simulation analysis
├── PHASE_3_FOG_PROCESSING_REPORT.md        # Architectures B, C, and D comparative analysis
├── PHASE_4_EXPERIMENTS_REPORT.md           # Controlled parameter sweep experiments (N=10-50, W=1-20, WAN stress)
├── PHASE_5_ANALYSIS_REPORT.md              # Metric analysis and graph verification report
│
├── simulator/
│   ├── build.ps1                           # Automated compiler for iFogSim2 core
│   ├── run_example.ps1                     # Canonical example launcher (VRGame, DCNS, Cardiovascular)
│   └── iFogSim/                            # Cloned iFogSim2 simulator core and external dependency JARs
│
├── src/
│   ├── baseline/                           # Architecture A: Cloud-only monitoring (HealthcareCloudOnly.java)
│   ├── fog_forwarding/                     # Architecture B: Fog forwarding proxy (HealthcareFogForwarding.java)
│   ├── fog_filtering/                      # Architecture C: Delta filter & window aggregator (HealthcareFogFiltering.java)
│   ├── priority_aware/                     # Architecture D: Priority bypass & artifact suppression (HealthcarePriorityFog.java)
│   ├── multi_ward/                         # Architecture E: Multi-ward two-gateway failover (HealthcareMultiWardFailover.java)
│   └── common/                             # Shared simulation core (Sensors, Actuators, MetricsTracker, Config)
│
├── dashboard/
│   ├── index.html                          # Single-page 3D hospital ward studio & layman architecture explorer
│   ├── server.py                           # Python localhost web server with live /api/run execution engine
│   └── start_dashboard.ps1                 # Launch script for http://localhost:8000
│
├── experiments/
│   └── run_all_experiments.ps1             # Master automation script for Experiments 1 through 5
│
├── results/
│   ├── csv/                                # Processed experimental datasets (exp1–exp5) and summary CSVs
│   ├── raw/                                # Raw 4,000+ reading event logs per architecture
│   ├── graphs/                             # 7 publication-quality figures (300 DPI PNG + vector PDF)
│   └── processed/                          # Summary tables in Markdown and IEEE LaTeX format
│
├── analysis/
│   ├── generate_analysis.py                # Python statistical analysis and figure generation script
│   └── export_docx_and_pptx.py             # Formatted Word report (.docx) and presentation deck (.pptx) generator
│
├── report/
│   ├── Topic19_IEEE_Research_Report.docx   # Complete IEEE research report in Microsoft Word format
│   ├── ieee_paper.tex                      # IEEE Transactions LaTeX source code (two-column format)
│   ├── ieee_paper.md                       # Full paper in Markdown
│   └── references.bib                      # Top 3 verified Q1 academic citations (iFogSim2, UT-GATE, BodyEdge)
│
└── presentation/
    ├── Topic19_Healthcare_Fog_Presentation.pptx # 14-slide widescreen presentation deck
    ├── presentation_slides.md              # Slide deck outline and presenter speaking notes
    └── viva_defense_guide.md               # Exhaustive viva defense question & answer guide
```

---

## 5. Detailed Usage Instructions

### Run Individual Architectures

```powershell
# Architecture A: Cloud-Only Baseline
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Arch A -NumPatients 10

# Architecture B: Fog Forwarding Overhead
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Arch B -NumPatients 10

# Architecture C: Naive Filtering & Aggregation (Window Size W = 5)
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Arch C -NumPatients 10 -WindowSize 5

# Architecture D: Priority-Aware Fog Computing
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Arch D -NumPatients 10 -WindowSize 5

# Architecture E: Multi-Ward Two-Gateway Failover (Experiment 5 Outage: 40s–60s)
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Arch E -NumPatients 10 -WindowSize 5
```

### Run Controlled Experimental Suite

```powershell
powershell -ExecutionPolicy Bypass -File .\experiments\run_all_experiments.ps1
```

This automates the entire evaluation matrix:
* **Experiment 1:** Architecture comparison (A vs. B vs. C vs. D vs. E).
* **Experiment 2:** Patient scalability scaling ($N = 10, 20, 30, 50$ patients, up to 200 sensors).
* **Experiment 3:** Aggregation window sensitivity ($W = 1, 2, 5, 10, 20$).
* **Experiment 4:** WAN network degradation under severe latency and jitter ($100\text{ ms} \to 300\text{ ms}$).
* **Experiment 5:** Scheduled gateway outage and inter-ward LAN failover resilience.

### Generate Publication Figures & Documents

```powershell
# 1. Regenerate 7 IEEE publication figures and LaTeX tables:
python analysis/generate_analysis.py

# 2. Re-export formatted Word report and PowerPoint presentation:
python analysis/export_docx_and_pptx.py
```

---

## 6. Interactive 3D Ward Visualizer (`http://localhost:8000`)

Start the visualizer server using:
```powershell
powershell -ExecutionPolicy Bypass -File .\dashboard\start_dashboard.ps1
```

### Visualizer Features
1. **Clean Clinical White/Light Theme:** Designed for academic defense presentations with high contrast and clear visual hierarchy.
2. **Interactive 3D Hospital Floorplan:**
   * **ICU Ward A:** 3 patient beds with active vital screens + **Gateway A** server rack with **5 glowing buffer slots** (`1/5 -> 5/5`) illustrating sliding-window batching.
   * **ICU Ward B:** 3 patient beds + **Gateway B** server rack.
   * **1.0 ms Inter-Ward LAN Bridge:** Glowing high-speed connection between Gateway A and Gateway B.
   * **Nurse Station Console:** Central alarm desk with a 3D ring that pulses bright red on emergency alerts.
   * **Remote Cloud Datacenter:** Tower connected via dashed 100 ms WAN links.
3. **Interactive Controls:**
   * `Play/Pause`, `Step (1 Tick)` for slow-motion analysis, and `Speed Slider` ($0.25\times$, $1.0\times$, $2.0\times$).
   * `Inject Cardiac Alert (Bed A1)` to trigger immediate priority dispatch.
   * `Simulate Gateway A Outage` to trigger real-time inter-ward LAN rerouting to Gateway B.
4. **Clickable 3D Node Inspector:** Click any 3D bed, gateway, console, or cloud datacenter to inspect hardware specs, link latencies, and live queue states.
5. **Layman Deep-Dive Guides:** Dedicated tabs for Architectures A, B, C, D, and E with real-world analogies, flow sequence cards, data generation specs, and math formulas.
6. **Live Java Execution Engine:** Select architecture, patient count, and window size, and click **"Run Live iFogSim Simulation"** to execute the Java engine directly from the browser with real-time console streaming.

---

## 7. Deliverables & Research Artifacts

* **IEEE Two-Column Research Paper:** [report/ieee_paper.tex](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/report/ieee_paper.tex) and [report/Topic19_IEEE_Research_Report.docx](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/report/Topic19_IEEE_Research_Report.docx)
* **Presentation Slide Deck (16:9 Widescreen):** [presentation/Topic19_Healthcare_Fog_Presentation.pptx](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/presentation/Topic19_Healthcare_Fog_Presentation.pptx) and [presentation/presentation_slides.md](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/presentation/presentation_slides.md)
* **Viva Defense Guide:** [presentation/viva_defense_guide.md](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/presentation/viva_defense_guide.md)
* **Experimental CSV Datasets:** [results/csv/](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/results/csv/)
* **Publication Figures (PNG & PDF):** [results/graphs/](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/results/graphs/)

---

## References

1. **R. Mahmud, Faheem, and R. Buyya**, "iFogSim2: An Extended iFogSim Simulator for Mobility, Clustering, and Microservice Management in Edge and Fog Computing Environments," *Journal of Systems and Software*, vol. 190, p. 111351, 2022. [https://doi.org/10.1016/j.jss.2022.111351](https://doi.org/10.1016/j.jss.2022.111351)
2. **A. M. Rahmani, T. N. Gia, B. Negash, A. Anzanpour, I. Azimi, M. Jiang, and P. Liljeberg**, "Exploiting smart e-Health gateways at the edge of healthcare Internet-of-Things: A fog computing approach," *Future Generation Computer Systems*, vol. 78, pp. 641–658, 2018. [https://doi.org/10.1016/j.future.2017.02.014](https://doi.org/10.1016/j.future.2017.02.014)
3. **P. Pace, G. Aloi, R. Gravina, G. Caliciuri, G. Fortino, and A. Liotta**, "An Edge-Based Architecture to Support Efficient Applications for Healthcare Industry 4.0," *IEEE Transactions on Industrial Informatics*, vol. 15, no. 1, pp. 481–489, Jan. 2019. [https://doi.org/10.1109/TII.2018.2843169](https://doi.org/10.1109/TII.2018.2843169)
