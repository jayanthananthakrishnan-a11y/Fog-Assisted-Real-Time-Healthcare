# Fog-Assisted Real-Time Healthcare Monitoring System (iFogSim)

**Topic 19 — Cloud Computing Research Project (15 Marks)**  
**Target:** 2025–2027 Edge/Fog Healthcare Architecture Research  
**Tool:** iFogSim2 / CloudSim 7  

---

## 1. Project Overview

Continuous physiological streams (Heart Rate, SpO2, Temperature, Blood Pressure) cannot tolerate high cloud latency and consume excessive network bandwidth if transmitted raw.

This research project evaluates an end-to-end architecture that combines:
- **Local Fog Processing:** Filtering, Aggregation, and Priority Classification with Critical-Reading Bypass at hospital fog gateways.
- **Centralized Cloud Analytics:** Deep clinical trend analysis and electronic health record archiving at the cloud datacenter.

### Evaluation Metrics
1. **End-to-End Latency (ms):** Generation to destination delivery delay for normal vs. critical readings.
2. **Bandwidth Saving (%):** Percentage reduction in cloud-bound network traffic.
3. **Reliability (%):** Successful delivery ratio of critical clinical alerts under varying loads.

---

## 2. Architectures to Evaluate

- **Architecture A (Baseline):** Sensors → Cloud → Cloud Analytics (Raw cloud-only streaming).
- **Architecture B:** Sensors → Fog Forwarding → Cloud → Cloud Analytics (Intermediate fog hop without processing).
- **Architecture C (Prescribed Solution):** Sensors → Fog Filtering/Aggregation → Cloud Analytics.
- **Architecture D (Enhanced Solution):** Sensors → Priority Classification → Fog Filtering/Aggregation OR Immediate Critical Bypass → Cloud Analytics.

---

## 3. Repository Organization

```text
FogHealthcareResearch/
│
├── project_guidelines.txt                   # Official professor-provided requirements
├── PHASE_1_SETUP_REPORT.md                 # Full Phase 1 exploration and validation report
├── setup_workspace.ps1                     # Workspace directory initialization script
│
├── simulator/
│   ├── build.ps1                           # Automated javac build script
│   ├── run_example.ps1                     # Simulation launcher
│   └── iFogSim/                            # Cloned iFogSim2 source (Cloudslab/iFogSim)
│       ├── jars/                           # Bundled external JAR dependencies
│       ├── src/org/                        # Simulator source code
│       └── out/production/iFogSim/         # Compiled class binaries
│
├── src/
│   ├── baseline/                           # Phase 2: Architecture A (Cloud-only)
│   ├── fog_forwarding/                     # Phase 3: Architecture B (Fog forwarding)
│   ├── fog_filtering/                      # Phase 3: Architecture C (Filtering & aggregation)
│   └── priority_aware/                     # Phase 3: Architecture D (Priority bypass)
│
├── experiments/                            # Phase 4: Controlled experimental runners
├── results/                                # Phase 4 & 5: Raw, CSV, and graph outputs
├── analysis/                               # Phase 5: Python visualization and analysis scripts
├── research/                               # Related work and literature notes
├── report/                                 # Phase 6: IEEE research paper (5–7 pages)
└── presentation/                           # Phase 6: Viva slide deck and presentation
```

---

## 4. Phase 1 Setup & Verification Instructions

### Prerequisites
- **JDK:** Java 21+ or Java 26 (tested with Oracle JDK 26.0.2).
- **Python:** Python 3.11+ (for experimental plotting in Phase 5).
- **Git:** Git 2.5+

### Build the Simulator
To compile all 445 source files using the verified PowerShell script:
```powershell
powershell -ExecutionPolicy Bypass -File simulator/build.ps1
```

### Run Existing Benchmark Examples
To run the canonical VRGame benchmark:
```powershell
powershell -ExecutionPolicy Bypass -File simulator/run_example.ps1 -ClassName org.fog.test.perfeval.VRGameFog
```

To run the DCNS benchmark:
```powershell
powershell -ExecutionPolicy Bypass -File simulator/run_example.ps1 -ClassName org.fog.test.perfeval.DCNSFog
```

To run the Cardiovascular Health Monitoring benchmark:
```powershell
powershell -ExecutionPolicy Bypass -File simulator/run_example.ps1 -ClassName org.fog.test.perfeval.CardiovascularHealthMonitoringApplication
```

### Run Simulations
To compile and run all four architectures sequentially and display the comparative summary:
```powershell
powershell -ExecutionPolicy Bypass -File build_and_run.ps1 -Arch ALL -NumPatients 10 -WindowSize 5
```

To run individual architectures:
```powershell
powershell -ExecutionPolicy Bypass -File build_and_run.ps1 -Arch A # Cloud-Only Baseline
powershell -ExecutionPolicy Bypass -File build_and_run.ps1 -Arch B # Fog Forwarding
powershell -ExecutionPolicy Bypass -File build_and_run.ps1 -Arch C -WindowSize 5 # Fog Filtering & Aggregation
powershell -ExecutionPolicy Bypass -File build_and_run.ps1 -Arch D -WindowSize 5 # Priority-Aware Fog
```

### Run Statistical Analysis & Figure Generation
To generate the 7 IEEE publication figures (PNG and PDF) and summary tables (Markdown and LaTeX) from the experimental CSVs:
```powershell
python analysis/generate_analysis.py
```

### Export Word (.docx) Report and PowerPoint (.pptx) Presentation
To generate the formatted Word research report and the 14-slide widescreen presentation deck:
```powershell
python analysis/export_docx_and_pptx.py
```

### Launch Interactive 3D Simulation Visualizer (Viva Demo)
To launch the interactive localhost web visualizer with real-time 3D topology and one-click Java simulation execution:
```powershell
powershell -ExecutionPolicy Bypass -File dashboard/start_dashboard.ps1
# Or run directly:
python dashboard/server.py
```
Open [http://localhost:8000](http://localhost:8000) in your web browser.

---

## 5. Current Progress
- [x] **Phase 1:** Environment setup, simulator selection, compilation, and canonical example validation. See [PHASE_1_SETUP_REPORT.md](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/PHASE_1_SETUP_REPORT.md).
- [x] **Phase 2:** Healthcare baseline implementation (Architecture A: Cloud-only) — Verified 4,000 readings, 207.711 ms avg latency, 1,953.13 KB cloud traffic. See [PHASE_2_BASELINE_REPORT.md](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/PHASE_2_BASELINE_REPORT.md).
- [x] **Phase 3:** Fog processing architectures (Architectures B, C, D) — Verified 77.45% bandwidth savings in Arch C, and 7.129 ms emergency critical response in Arch D (96.57% reduction). See [PHASE_3_FOG_PROCESSING_REPORT.md](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/PHASE_3_FOG_PROCESSING_REPORT.md).
- [x] **Phase 4:** Controlled scalability and comparative parameter sweep experiments (N = 10, 20, 30, 50; W = 1, 2, 5, 10, 20; WAN Stress profiles) — Verified 100% mathematical accounting, flat 7.129 ms critical response, and 100% critical SLA compliance under network degradation. See [PHASE_4_EXPERIMENTS_REPORT.md](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/PHASE_4_EXPERIMENTS_REPORT.md).
- [x] **Phase 5:** Metric analysis, CSV exports, and publication-quality graph generation — Generated 7 IEEE figures (300 DPI PNG + vector PDF) and summary tables (Markdown and IEEE LaTeX `table*` format) with 100% empirical data fidelity. See [PHASE_5_ANALYSIS_REPORT.md](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/PHASE_5_ANALYSIS_REPORT.md).
- [x] **Phase 6:** Literature review, IEEE 5–7 page research paper, presentation deck, and viva defense guide — Curated 10 real academic citations, authored full IEEE two-column paper ([Word DOCX](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/report/Topic19_IEEE_Research_Report.docx), [LaTeX](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/report/ieee_paper.tex), [Markdown](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/report/ieee_paper.md), and [BibTeX](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/report/references.bib)), produced a 14-slide widescreen presentation deck ([PowerPoint PPTX](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/presentation/Topic19_Healthcare_Fog_Presentation.pptx) and [Markdown](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/presentation/presentation_slides.md)), and compiled an exhaustive viva defense guide ([viva_defense_guide.md](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/presentation/viva_defense_guide.md)). See [literature_review.md](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/research/literature_review.md).
- [x] **Viva Demonstration Visualizer:** Interactive Three.js 3D spatial network topology visualizer and lightweight Python server in `dashboard/` with preloaded Phase 4 verified numbers, dynamic topology animations, and real-time Java simulation execution.
