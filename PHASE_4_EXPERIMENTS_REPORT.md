# PHASE 4 REPORT: Controlled Experiments & Parameter Sweeps

**Project Title:** Fog-Assisted Real-Time Healthcare / Smart-City Monitoring System (iFogSim)  
**Topic:** Topic 19  
**Team:** Undergraduate Research Team (3 Members)  
**Phase:** Phase 4 of 6  
**Status:** COMPLETE (All 4 controlled experiments executed, verified, and exported to structured CSV datasets)

---

## 1. Executive Summary & Verification of Pre-Experiment Fixes

In Phase 4, we conducted four rigorous, reproducible experiments across all four architectures (A, B, C, D) using our automated runner (`experiments/run_all_experiments.ps1`) under fixed deterministic random seeding ($seed = 42\text{L}$).

### Verification of Pre-Experiment Fixes
1. **End-of-Simulation Buffer Flush:**
   - In Phase 3, trailing partial windows (< $W$) remained in edge buffers when sensors stopped transmitting.
   - We scheduled a final buffer flush event at $t = \text{SIMULATION\_DURATION} + 500.0\text{ ms}$ ($100{,}500\text{ ms}$) and expanded the drain window to $115{,}000\text{ ms}$.
   - **Verification Result:** Across every scale point and architecture, mathematical accounting is **exactly 100.00%**:
     $$\text{Filtered at Fog} + \text{Delivered} = \text{Total Generated Readings}$$
     - *10 Patients (4,000 Readings):* Arch C ($593 + 3{,}407 = 4{,}000$), Arch D ($712 + 3{,}288 = 4{,}000$).
     - *20 Patients (8,000 Readings):* Arch C ($1{,}168 + 6{,}832 = 8{,}000$), Arch D ($1{,}394 + 6{,}606 = 8{,}000$).
     - *30 Patients (12,000 Readings):* Arch C ($1{,}821 + 10{,}179 = 12{,}000$), Arch D ($2{,}116 + 9{,}884 = 12{,}000$).
     - *50 Patients (20,000 Readings):* Arch C ($3{,}098 + 16{,}902 = 20{,}000$), Arch D ($3{,}611 + 16{,}389 = 20{,}000$).
2. **Clean `README.md` Checklist:**
   - Section 5 of `README.md` was streamlined with all completed milestones verified and aligned.

---

## 2. Experiment 1: Architecture Comparison (Fixed Workload)

**Configuration:** $N = 10$ Patients ($40$ vital-sign sensors), Window Size $W = 5$, WAN Latency $= 100$ ms, WAN Bandwidth $= 10{,}000$ Mbps.

### Empirical Results Table (`results/csv/exp1_architecture_comparison.csv`)

| Metric | Architecture A (Cloud-Only Baseline) | Architecture B (Fog Forwarding) | Architecture C (Fog Filtering & Aggregation) | Architecture D (Priority-Aware Fog) |
|---|---|---|---|---|
| **Total Readings Generated** | $4{,}000$ | $4{,}000$ | $4{,}000$ | $4{,}000$ |
| **Filtered (Dropped at Fog)** | $0$ ($0.0\%$) | $0$ ($0.0\%$) | **$593$** ($14.83\%$) | **$712$** ($17.80\%$) |
| **Delivered Readings** | $4{,}000$ ($100.0\%$) | $4{,}000$ ($100.0\%$) | $3{,}407$ ($100.0\%$) | $3{,}288$ ($100.0\%$) |
| **Critical Readings Generated** | $226$ | $226$ | $226$ | $226$ |
| **Critical Readings Delivered** | $226$ / $226$ ($100.0\%$) | $226$ / $226$ ($100.0\%$) | $226$ / $226$ ($100.0\%$) | **$226$ / $226$ ($100.0\%$)** |
| **Critical Delivery Rate** | $100.00\%$ | $100.00\%$ | $100.00\%$ | **$100.00\%$** |
| **Average End-to-End Latency** | **$207.711$ ms** | $211.432$ ms | $2{,}528.443$ ms | $2{,}604.071$ ms |
| **Normal Reading Latency** | $207.704$ ms | $211.424$ ms | $2{,}565.268$ ms | $3{,}182.284$ ms |
| **Abnormal Reading Latency** | $207.633$ ms | $211.366$ ms | $2{,}404.344$ ms | **$208.468$ ms** |
| **Critical Reading Latency** | $207.954$ ms | $211.673$ ms | $2{,}293.515$ ms | **$7.129$ ms (96.57% Faster)** |
| **P95 Latency** | $215.111$ ms | $218.685$ ms | $5{,}207.611$ ms | $7{,}207.817$ ms |
| **Cloud-Bound Traffic** | $1{,}953.13$ KB | $1{,}953.13$ KB | **$444.97$ KB** | **$651.90$ KB** |
| **Total Network Traffic** | $5{,}468.75$ KB | $5{,}468.75$ KB | **$2{,}671.92$ KB** | **$3{,}018.31$ KB** |
| **Cloud Bandwidth Saving vs A** | $0.00\%$ | $0.00\%$ | **$77.22\%$** | **$66.62\%$** |
| **Total Bandwidth Saving vs A** | $0.00\%$ | $0.00\%$ | **$51.14\%$** | **$44.81\%$** |
| **Critical SLA Delivered ($\le 500$ms)**| $226$ / $226$ | $226$ / $226$ | $63$ / $226$ | **$226$ / $226$** |
| **Critical SLA Reliability** | $100.00\%$ | $100.00\%$ | $27.88\%$ (Severe Failure) | **$100.00\%$ (Perfect Compliance)** |

### Key Observations
- Architecture B isolates the pure edge forwarder overhead ($+3.72\text{ ms}$ latency, $0\%$ bandwidth savings).
- Architecture C reduces cloud-bound bandwidth by **$77.22\%$**, but drops Critical SLA Reliability down to **$27.88\%$** because critical vitals are trapped in aggregation buffers.
- Architecture D achieves the optimal trade-off: **$66.62\%$ cloud bandwidth savings** while maintaining **$100.00\%$ Critical SLA Reliability** with an extraordinary **$7.129\text{ ms}$** emergency delivery time.

---

## 3. Experiment 2: Patient & Sensor Scalability ($N \in \{10, 20, 30, 50\}$)

**Configuration:** Window Size $W = 5$, WAN Latency $= 100$ ms, WAN Bandwidth $= 10{,}000$ Mbps. Sweep $N \in \{10, 20, 30, 50\}$ Patients ($40, 80, 120, 200$ sensors).

### Empirical Results Table (`results/csv/exp2_scalability.csv`)

| Architecture | Patients ($N$) | Sensors | Total Generated | Filtered at Fog | Total Delivered | Critical Generated | Critical Delivered | Critical Latency (ms) | Avg Latency (ms) | Cloud Traffic (KB) | Cloud BW Saving (%) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **Arch A (Cloud-Only)** | 10 | 40 | 4,000 | 0 | 4,000 | 226 | 226 | 207.954 | 207.711 | 1,953.13 | 0.00% |
| **Arch B (Fog Forward)** | 10 | 40 | 4,000 | 0 | 4,000 | 226 | 226 | 211.673 | 211.432 | 1,953.13 | 0.00% |
| **Arch C (Filter & Agg)**| 10 | 40 | 4,000 | 593 | 3,407 | 226 | 226 | 2,293.515 | 2,528.443 | 444.97 | **77.22%** |
| **Arch D (Priority Fog)**| 10 | 40 | 4,000 | 712 | 3,288 | 226 | 226 | **7.129** | 2,604.071 | 651.90 | **66.62%** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **Arch A (Cloud-Only)** | 20 | 80 | 8,000 | 0 | 8,000 | 433 | 433 | 209.899 | 209.747 | 3,906.25 | 0.00% |
| **Arch B (Fog Forward)** | 20 | 80 | 8,000 | 0 | 8,000 | 433 | 433 | 213.236 | 213.056 | 3,906.25 | 0.00% |
| **Arch C (Filter & Agg)**| 20 | 80 | 8,000 | 1,168 | 6,832 | 433 | 433 | 2,311.691 | 2,536.424 | 888.04 | **77.27%** |
| **Arch D (Priority Fog)**| 20 | 80 | 8,000 | 1,394 | 6,606 | 433 | 433 | **7.129** | 2,590.063 | 1,305.57 | **66.58%** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **Arch A (Cloud-Only)** | 30 | 120 | 12,000 | 0 | 12,000 | 622 | 622 | 211.769 | 211.783 | 5,859.38 | 0.00% |
| **Arch B (Fog Forward)** | 30 | 120 | 12,000 | 0 | 12,000 | 622 | 622 | 214.682 | 214.680 | 5,859.38 | 0.00% |
| **Arch C (Filter & Agg)**| 30 | 120 | 12,000 | 1,821 | 10,179 | 622 | 622 | 2,378.269 | 2,556.235 | 1,319.04 | **77.49%** |
| **Arch D (Priority Fog)**| 30 | 120 | 12,000 | 2,116 | 9,884 | 622 | 622 | **7.126** | 2,602.477 | 1,939.06 | **66.91%** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **Arch A (Cloud-Only)** | 50 | 200 | 20,000 | 0 | 20,000 | 1,025 | 1,025 | 215.796 | 215.854 | 9,765.63 | 0.00% |
| **Arch B (Fog Forward)** | 50 | 200 | 20,000 | 0 | 20,000 | 1,025 | 1,025 | 217.905 | 217.930 | 9,765.63 | 0.00% |
| **Arch C (Filter & Agg)**| 50 | 200 | 20,000 | 3,098 | 16,902 | 1,025 | 1,025 | 2,419.802 | 2,567.771 | 2,195.65 | **77.52%** |
| **Arch D (Priority Fog)**| 50 | 200 | 20,000 | 3,611 | 16,389 | 1,025 | 1,025 | **7.125** | 2,619.266 | 3,221.48 | **67.01%** |

### Key Scalability Trends
1. **Critical Latency Invariance in Architecture D:**
   - As load increases from $40$ to $200$ sensors ($5\times$ expansion), Architecture D's critical emergency delivery latency remains rock-solid at **$7.125\text{--}7.129\text{ ms}$**. Because critical bypass logic operates on the edge LAN, it is immune to cloud WAN queue growth.
2. **Cloud Traffic Growth:**
   - At $50$ patients, Architecture A pumps **$9.77$ MB** over the WAN. Architecture C suppresses this to **$2.20$ MB** ($77.52\%$ saving), and Architecture D suppresses it to **$3.22$ MB** ($67.01\%$ saving).
3. **Linear Bandwidth Savings:**
   - Cloud bandwidth saving percentage remains strictly constant across all patient scales ($\approx 77.3\text{--}77.5\%$ for Arch C, $\approx 66.6\text{--}67.0\%$ for Arch D), verifying that edge filtering algorithms scale perfectly with linear patient growth.

---

## 4. Experiment 3: Aggregation Window ($W$) Sensitivity Sweep

**Configuration:** $N = 20$ Patients ($80$ sensors), WAN Latency $= 100$ ms. Sweep Window Size $W \in \{2, 5, 10, 20\}$ across Architectures C and D, with Architecture A ($W=1$) as baseline reference.

### Empirical Results Table (`results/csv/exp3_aggregation_window.csv`)

| Architecture | Window Size ($W$) | Critical Latency (ms) | Abnormal Latency (ms) | Normal Latency (ms) | Avg Latency (ms) | Cloud Traffic (KB) | Cloud BW Saving (%) | Critical SLA Reliability (%) |
|---|---|---|---|---|---|---|---|---|
| **Arch A (Reference Baseline)** | 1 | 209.899 | 209.692 | 209.744 | **209.747** | 3,906.25 | **0.00%** | **100.00%** |
| **Arch C (Filter & Agg)** | 2 | 740.349 | 752.832 | 801.980 | 792.297 | 2,182.32 | 44.13% | 46.42% |
| **Arch D (Priority Fog)** | 2 | **7.129** | 208.714 | 952.062 | 799.766 | 2,318.02 | 40.66% | **100.00%** |
|---|---|---|---|---|---|---|---|---|
| **Arch C (Filter & Agg)** | 5 | 2,311.691 | 2,406.190 | 2,572.502 | 2,536.424 | 888.04 | 77.27% | 22.86% |
| **Arch D (Priority Fog)** | 5 | **7.129** | 208.344 | 3,154.482 | 2,590.063 | 1,305.57 | 66.58% | **100.00%** |
|---|---|---|---|---|---|---|---|---|
| **Arch C (Filter & Agg)** | 10 | 5,106.601 | 5,427.256 | 5,391.465 | 5,377.618 | 460.84 | 88.20% | 11.78% |
| **Arch D (Priority Fog)** | 10 | **7.129** | 208.193 | 6,782.217 | 5,539.021 | 966.60 | 75.26% | **100.00%** |
|---|---|---|---|---|---|---|---|---|
| **Arch C (Filter & Agg)** | 20 | 10,551.186 | 10,978.964 | 10,890.657 | 10,879.521 | 240.58 | 93.84% | 4.62% |
| **Arch D (Priority Fog)** | 20 | **7.129** | 208.077 | 13,719.517 | 11,178.319 | 795.21 | 79.64% | **100.00%** |

### Pareto Trade-Off Analysis
1. **The Window Scaling Penalty in Architecture C:**
   - In Architecture C, every doubling of window size $W$ cuts cloud bandwidth in half (from $2{,}182\text{ KB}$ at $W=2$ down to $240.6\text{ KB}$ at $W=20$, reaching **$93.84\%$ savings**).
   - However, Critical reading latency scales directly with $W$: from $740\text{ ms}$ at $W=2$ to **$10{,}551\text{ ms}$ ($10.5\text{ seconds}$)** at $W=20$. Critical SLA compliance drops to an abysmal **$4.62\%$**.
2. **The Priority-Aware Decoupling in Architecture D:**
   - In Architecture D, changing $W$ from $2$ to $20$ has **zero impact on Critical vitals ($7.129\text{ ms}$)** and **zero impact on Abnormal vitals ($208.1\text{ ms}$)**.
   - Only routine Normal vitals absorb the buffering delay ($952\text{ ms} \to 13{,}719\text{ ms}$), enabling hospitals to safely configure larger window sizes (e.g. $W=10$ or $W=20$) to save up to **$79.64\%$ of cloud bandwidth** without compromising clinical safety.

---

## 4. Experiment 4: WAN Network-Stress & Clinical Reliability

**Configuration:** $N = 20$ Patients ($80$ sensors), Window Size $W = 5$. Sweep WAN conditions across four real-world link profiles:
1. **`Fiber_Normal`:** WAN bandwidth $= 100{,}000$ Kbps ($100$ Mbps), WAN latency $= 100$ ms.
2. **`WAN_Congested`:** WAN bandwidth $= 1{,}000$ Kbps ($1$ Mbps), WAN latency $= 200$ ms.
3. **`WAN_Severe_Bottleneck`:** WAN bandwidth $= 250$ Kbps, WAN latency $= 400$ ms.
4. **`WAN_Rural_Degraded`:** WAN bandwidth $= 100$ Kbps, WAN latency $= 600$ ms.
- **Clinical Emergency SLA Deadline:** $500.0$ ms.

### Empirical Results Table (`results/csv/exp4_network_stress.csv`)

| Network Profile | Architecture | WAN Latency (ms) | WAN Bw (Kbps) | Critical Latency (ms) | Avg Latency (ms) | Cloud Traffic (KB) | Cloud BW Saving (%) | Critical SLA Delivered | Critical SLA Reliability (%) |
|---|---|---|---|---|---|---|---|---|---|
| **Fiber_Normal** | **Arch A (Cloud-Only)** | 100.0 | 100,000 | 209.899 | 209.747 | 3,906.25 | 0.00% | 433 / 433 | **100.00%** |
| **Fiber_Normal** | **Arch C (Filter & Agg)**| 100.0 | 100,000 | 2,311.691 | 2,536.424 | 888.04 | 77.27% | 99 / 433 | 22.86% |
| **Fiber_Normal** | **Arch D (Priority Fog)**| 100.0 | 100,000 | **7.129** | 2,590.063 | 1,305.57 | 66.58% | 433 / 433 | **100.00%** |
|---|---|---|---|---|---|---|---|---|---|
| **WAN_Congested**| **Arch A (Cloud-Only)** | 200.0 | 1,000 | 419.337 | 418.932 | 3,906.25 | 0.00% | 433 / 433 | **100.00%** |
| **WAN_Congested**| **Arch C (Filter & Agg)**| 200.0 | 1,000 | 2,560.482 | 2,784.159 | 888.04 | 77.27% | 93 / 433 | 21.48% |
| **WAN_Congested**| **Arch D (Priority Fog)**| 200.0 | 1,000 | **7.129** | 2,833.101 | 1,305.57 | 66.58% | 433 / 433 | **100.00%** |
|---|---|---|---|---|---|---|---|---|---|
| **WAN_Severe** | **Arch A (Cloud-Only)** | 400.0 | 250 | 855.311 | 854.068 | 3,906.25 | 0.00% | 0 / 433 | **0.00% (FAILURE)** |
| **WAN_Severe** | **Arch C (Filter & Agg)**| 400.0 | 250 | 3,108.709 | 3,329.177 | 888.04 | 77.27% | 0 / 433 | **0.00% (FAILURE)** |
| **WAN_Severe** | **Arch D (Priority Fog)**| 400.0 | 250 | **7.129** | 3,379.659 | 1,305.57 | 66.58% | 433 / 433 | **100.00% (PERFECT)** |
|---|---|---|---|---|---|---|---|---|---|
| **WAN_Rural** | **Arch A (Cloud-Only)** | 600.0 | 100 | 1,317.167 | 1,314.068 | 3,906.25 | 0.00% | 0 / 433 | **0.00% (FAILURE)** |
| **WAN_Rural** | **Arch C (Filter & Agg)**| 600.0 | 100 | 3,848.779 | 4,059.776 | 888.04 | 77.27% | 0 / 433 | **0.00% (FAILURE)** |
| **WAN_Rural** | **Arch D (Priority Fog)**| 600.0 | 100 | **7.129** | 7,651.278 | 1,305.57 | 66.58% | 433 / 433 | **100.00% (PERFECT)** |

### Clinical Reliability Analysis
1. **The Sudden Collapse of Cloud-Only (Architecture A):**
   - Under normal fiber and light congestion, Architecture A meets the $500\text{ ms}$ clinical SLA ($209.9\text{ ms}$ and $419.3\text{ ms}$).
   - However, once the WAN link bottlenecks to $250\text{ Kbps}$ or experiences $400\text{--}600\text{ ms}$ WAN latency (common in rural clinics or emergency situations), Architecture A's critical latency shoots up to **$855.3\text{ ms}$ and $1{,}317.2\text{ ms}$**.
   - As a result, **Critical SLA Reliability drops to exactly 0.00%**, risking patient lives.
2. **The Failure of Unaware Aggregation (Architecture C):**
   - Architecture C fails the emergency SLA across all network conditions (reliability never exceeds $22.86\%$, dropping to $0.00\%$ under stress) because window buffering delay alone exceeds the $500\text{ ms}$ window.
3. **The Unshakable Reliability of Priority-Aware Fog (Architecture D):**
   - Because critical readings bypass the WAN entirely and trigger emergency alerts over the local hospital LAN, Architecture D's critical latency is **$7.129\text{ ms}$ in every single network scenario**.
   - Even when the WAN degrades to a $100\text{ Kbps}$ crawl with $600\text{ ms}$ latency, Architecture D delivers **100.00% Critical SLA Reliability** (433/433 critical alerts delivered in $< 10$ ms).

---

## 5. Artifacts Generated and Verified

All Phase 4 datasets are stored in `results/csv/`:

| Dataset File | File Size | Description |
|---|---|---|
| [exp1_architecture_comparison.csv](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/results/csv/exp1_architecture_comparison.csv) | 1.1 KB | Standardized 4-architecture benchmark at $N=10$, $W=5$ |
| [exp2_scalability.csv](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/results/csv/exp2_scalability.csv) | 4.2 KB | 16-run scalability sweep ($N \in \{10, 20, 30, 50\}$ patients) |
| [exp3_aggregation_window.csv](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/results/csv/exp3_aggregation_window.csv) | 2.5 KB | 9-run sensitivity sweep ($W \in \{1, 2, 5, 10, 20\}$) |
| [exp4_network_stress.csv](file:///c:/Users/akris/OneDrive/Desktop/Cloud%20Computing%20Project/results/csv/exp4_network_stress.csv) | 3.5 KB | 12-run WAN network degradation and clinical SLA reliability sweep |

---

## 6. Viva Defense & Research Justification Guide

### Q1: How do you defend that Architecture D is truly superior if its average latency is higher than Architecture A?
**Answer:** Average latency is a misleading metric in healthcare because routine non-critical readings ($84.4\%$) dominate the average. What matters in clinical intensive care is **triage latency by severity**:
- For **Critical** readings, Architecture D delivers alerts in **$7.129\text{ ms}$** vs **$207.954\text{ ms}$** in Architecture A ($96.57\%$ faster).
- For **Normal** readings, taking $3$ seconds instead of $0.2$ seconds has zero negative clinical consequence, but saves **$66.62\%$ of cloud WAN traffic**. Architecture D prioritizes clinician response time where it matters while conserving expensive cloud infrastructure.

### Q2: Why does Architecture A fail the SLA deadline in Experiment 4 while Architecture D succeeds?
**Answer:** Architecture A is completely dependent on the wide-area network (WAN) link connecting the hospital to the remote cloud datacenter. When that link experiences congestion ($100\text{--}250\text{ Kbps}$) or latency spikes ($400\text{--}600\text{ ms}$), physical transmission and propagation delays push the round-trip latency past $850\text{--}1{,}300\text{ ms}$, missing the $500\text{ ms}$ emergency SLA. Architecture D uses **edge emergency dual dispatch**: the bedside alert is transmitted directly over the hospital LAN ($7.1\text{ ms}$), making it physically immune to external WAN degradation.

### Q3: What is the optimal aggregation window size $W$ for hospital deployment?
**Answer:** In Architecture C, choosing $W$ is an impossible dilemma: $W=2$ preserves some responsiveness ($740\text{ ms}$) but saves only $44\%$ bandwidth; $W=20$ saves $93.8\%$ bandwidth but causes deadly $10.5\text{ s}$ alert delays. In Architecture D, however, critical vitals are decoupled from $W$. A hospital can deploy $W=10$ or $W=20$, achieving **$\approx 80\%$ WAN bandwidth conservation** while keeping critical alert latency at **$7.1\text{ ms}$**.

---

## 7. Next Steps: Phase 5 (Analysis & Graph Generation)

Phase 4 is complete and verified. The datasets are ready for **Phase 5 (Statistical Analysis & Publication-Quality Graph Generation)**:
1. Develop Python scripts (`analysis/plot_experiments.py`) using `matplotlib` and `seaborn` to generate publication-ready IEEE-style figures:
   - **Figure 1:** End-to-End Latency vs. Patient Scale ($N = 10 \to 50$) by Architecture.
   - **Figure 2:** Cloud-Bound Bandwidth vs. Patient Scale ($N = 10 \to 50$).
   - **Figure 3:** Pareto Frontier: Cloud Bandwidth Saving (%) vs. Aggregation Window ($W = 1 \to 20$) with Critical Latency overlay.
   - **Figure 4:** Critical SLA Reliability (%) vs. WAN Network Degradation Profiles.
2. Export high-resolution PNG and PDF figures to `results/graphs/` for inclusion in the IEEE research report.
