# PHASE 3 REPORT: Fog Processing Architectures (Architectures B, C, and D)

**Project Title:** Fog-Assisted Real-Time Healthcare / Smart-City Monitoring System (iFogSim)  
**Topic:** Topic 19  
**Team:** Undergraduate Research Team (3 Members)  
**Phase:** Phase 3 of 6  
**Status:** COMPLETE (Architectures B, C, and D implemented, empirically benchmarked, and verified against Architecture A)

---

## 1. Executive Summary

In Phase 3, we successfully implemented, verified, and benchmarked all three fog-assisted architectures specified in Topic 19, executing each against the exact same deterministic patient workload ($seed = 42\text{L}$, $10$ patients, $40$ vital-sign sensors, $100$ seconds simulated time) established in Phase 2:

1. **Architecture A (Cloud-Only Baseline):** Sensors $\to$ Cloud Datacenter $\to$ Cloud Analytics $\to$ Bedside Display.
2. **Architecture B (Fog Forwarding):** Sensors $\to$ Hospital Fog Gateway (Forwarder) $\to$ WAN $\to$ Cloud Datacenter $\to$ Bedside Display.
3. **Architecture C (Fog Filtering & Aggregation):** Sensors $\to$ Hospital Fog Gateway (Redundancy Filter & Window Aggregator) $\to$ WAN $\to$ Cloud Datacenter $\to$ Bedside Display.
4. **Architecture D (Priority-Aware Fog Processing):** Sensors $\to$ Hospital Fog Gateway (Priority Classifier & Emergency Dual Dispatch) $\to$ Local Emergency Bedside Alert ($3\text{--}7\text{ ms}$) + Cloud Analytics.

---

### Comparative Empirical Summary Table

The table below reflects the exact simulation outputs gathered across all four architectures:

| Evaluation Metric | Architecture A (Cloud-Only Baseline) | Architecture B (Fog Forwarding) | Architecture C (Fog Filtering & Aggregation) | Architecture D (Priority-Aware Fog) | Best Performing Architecture |
|---|---|---|---|---|---|
| **Total Readings Generated** | $4{,}000$ | $4{,}000$ | $4{,}000$ | $4{,}000$ | Identical across all ($seed = 42\text{L}$) |
| **Normal Readings Generated** | $3{,}376$ ($84.4\%$) | $3{,}376$ ($84.4\%$) | $3{,}376$ ($84.4\%$) | $3{,}376$ ($84.4\%$) | Identical workload |
| **Abnormal Readings Generated** | $398$ ($10.0\%$) | $398$ ($10.0\%$) | $398$ ($10.0\%$) | $398$ ($10.0\%$) | Identical workload |
| **Critical Readings Generated** | $226$ ($5.7\%$) | $226$ ($5.7\%$) | $226$ ($5.7\%$) | $226$ ($5.7\%$) | Identical workload |
| **Filtered Readings (Dropped at Fog)** | $0$ ($0.0\%$) | $0$ ($0.0\%$) | **$593$** ($14.83\%$) | **$712$** ($17.80\%$) | **Arch D** (Higher normal filtering efficiency) |
| **Delivered Readings** | $4{,}000$ ($100.0\%$) | $4{,}000$ ($100.0\%$) | $3{,}380$ ($99.21\%$) | $3{,}264$ ($99.27\%$) | Arch A & B ($100\%$, no filtering) |
| **Critical Readings Delivered** | $226$ / $226$ ($100.0\%$) | $226$ / $226$ ($100.0\%$) | $226$ / $226$ ($100.0\%$) | **$226$ / $226$ ($100.0\%$)** | **100% Reliability Across All** |
| **Aggregated Batches Emitted ($W=5$)** | $0$ | $0$ | $694$ | $542$ | Arch D (Only Normal vitals batched) |
| **Critical Emergency Bypasses** | $0$ | $0$ | $0$ | **$226$** ($100\%$ critical) | **Arch D Exclusively** |
| **Average End-to-End Latency** | $207.711$ ms | $211.432$ ms | $2{,}518.473$ ms | $2{,}589.268$ ms | Arch A (Instant raw forward) |
| **Normal Reading Latency** | $207.704$ ms | $211.424$ ms | $2{,}560.835$ ms | $3{,}169.238$ ms | Arch A |
| **Abnormal Reading Latency** | $207.633$ ms | $211.365$ ms | $2{,}286.924$ ms | **$208.467$ ms** | **Arch D** (Expedited zero-wait bypass) |
| **Critical Reading Latency** | $207.954$ ms | $211.673$ ms | $2{,}287.762$ ms | **$7.129$ ms** | **Arch D (96.57% Latency Reduction)** |
| **P95 End-to-End Latency** | $215.111$ ms | $218.685$ ms | $5{,}207.591$ ms | $7{,}207.776$ ms | Arch A |
| **Cloud-Bound Network Traffic** | $1{,}953.13$ KB ($2{,}000{,}000$ B) | $1{,}953.13$ KB ($2{,}000{,}000$ B) | **$440.53$ KB** ($451{,}100$ B) | **$648.73$ KB** ($664{,}300$ B) | **Arch C (77.45% Cloud Bandwidth Saving)** |
| **Total Network Traffic** | $5{,}468.75$ KB ($5{,}600{,}000$ B) | $6{,}250.00$ KB ($6{,}400{,}000$ B) | **$2{,}361.91$ KB** ($2{,}418{,}600$ B) | **$3{,}013.18$ KB** ($3{,}085{,}500$ B) | **Arch C (56.81% Total Network Saving)** |
| **Cloud Bandwidth Saving vs Arch A** | $0.00\%$ | $0.00\%$ | **$77.45\%$** | **$66.79\%$** | **Arch C** (Aggregates all streams) |
| **Total Bandwidth Saving vs Arch A** | $0.00\%$ | $-14.29\%$ | **$56.81\%$** | **$44.90\%$** | **Arch C** |

---

## 2. Key Research Findings & Insights

### 1. The Pure Fog Gateway Overhead (Architecture B)
- Adding a physical hospital fog gateway sitting between bedside patient bridges and the remote cloud WAN link adds exactly **$3.721$ ms** of end-to-end latency ($211.432$ ms vs $207.711$ ms in Arch A).
- This extra delay precisely accounts for:
  $$\Delta_{\text{overhead}} = \text{Fog Processing (200 MIPS)} + \text{Internal LAN Switching} \approx 3.7\text{ ms}$$
- Architecture B consumes **$0.0\%$ cloud bandwidth savings** and slightly increases total network traffic ($-14.29\%$) due to the additional local hop. This confirms that introducing a fog node without edge intelligence offers zero architectural benefit.

### 2. The Bandwidth vs. Latency Dilemma (Architecture C)
- Architecture C introduces stateful edge filtering and temporal window aggregation ($W = 5$ readings):
  - **Bandwidth Reduction:** Cloud-bound WAN traffic plummeted from **$1{,}953.13$ KB** to **$440.53$ KB**—an outstanding **$77.45\%$ bandwidth conservation**.
  - **The Cost of Aggregation:** However, buffering readings to accumulate window $W=5$ increases average latency from $207.7$ ms to **$2{,}518.473$ ms**.
  - **The Safety Hazard:** In Architecture C, life-threatening **CRITICAL** readings suffered an average latency of **$2{,}287.762$ ms** because they were forced to sit in the edge buffer waiting for subsequent readings to accumulate. In a cardiac arrest scenario ($>130$ BPM), a $2.3$-second delay is clinically dangerous.

### 3. The Clinical Superiority of Priority-Aware Fog (Architecture D)
- Architecture D resolves the aggregation dilemma through a **Three-Tier Priority Architecture**:
  1. **Tier 1 (Critical Vitals):** When an acute condition is detected ($5.7\%$ of readings), the Fog Gateway triggers an **Emergency Local Bypass**, alerting the bedside clinician in **$7.129$ ms** ($96.57\%$ faster than the Cloud-Only baseline and $99.69\%$ faster than Architecture C), while concurrently logging to the cloud in the background.
  2. **Tier 2 (Abnormal Vitals):** Urgent readings bypass the aggregation window and are expedited immediately to the Cloud, achieving **$208.467$ ms** latency (identical to direct WAN transmission).
  3. **Tier 3 (Normal Vitals):** Routine vital signs ($84.4\%$) are aggressively filtered (redundancy suppression) and aggregated ($W=5$), preserving **$66.79\%$ of cloud bandwidth**.
- **Conclusion:** Architecture D provides the optimal operational trade-off: **near-instantaneous emergency response ($7.1$ ms) for life-threatening events with substantial WAN bandwidth conservation ($66.8\%$) for routine health streams.**

---

## 3. Detailed Architecture Specifications & Logic

### A. Architecture B — Fog Forwarding (`src/fog_forwarding/`)
- **Topology:**
  $$\text{Sensors} \xrightarrow{2\text{ms}} \text{Patient Gateway} \xrightarrow{2\text{ms}} \text{Hospital Fog Gateway (8,000 MIPS)} \xrightarrow{100\text{ms WAN}} \text{Cloud} \xrightarrow{100\text{ms WAN}} \text{Actuator}$$
- **Module:** `fog_forwarder` allocated on `hospital_fog_gateway`.
- **Behavior:** Inspects incoming raw readings (`FOG_FORWARD_CPU_LENGTH = 200` MIPS) and forwards $100\%$ of readings to `cloud_analytics` without modification.
- **Viva Note:** Serves as a scientific control to prove that bandwidth savings in C and D are caused by edge intelligence, not by topology topology differences.

---

### B. Architecture C — Fog Filtering & Window Aggregation (`src/fog_filtering/`)
- **Module:** `fog_filter_aggregator` hosted on `hospital_fog_gateway`.
- **Engine:** `FilterAggregatorEngine.java`.
- **Step 1 — Stateful Delta Filtering (Redundancy Suppression):**
  - Maintains the `lastForwardedValue` per `(patientId, sensorType)` tuple stream.
  - If a reading is `NORMAL` and $|\text{current} - \text{last}| < \delta_{\text{threshold}}$, it is **dropped at the fog gateway**:
    - Heart Rate: $\delta < 2.0$ BPM
    - SpO2: $\delta < 1.0\%$
    - Body Temperature: $\delta < 0.2\,^\circ\text{C}$
    - Blood Pressure: $\delta < 3.0$ mmHg
  - Result: **$593$ redundant readings** were dropped at the edge, saving $296.5$ KB of raw WAN transmission.
- **Step 2 — Sliding Window Aggregation ($W = 5$):**
  - Non-redundant readings are buffered into a per-stream queue.
  - Once $W=5$ readings accumulate (or upon simulation drain flush at $t \ge 99.2$s), a single aggregated summary tuple is generated:
    $$\text{Payload} = \{\text{count}=5, \text{avg}, \text{min}, \text{max}\} \quad (\text{Size} = 650\text{ Bytes}, \text{CPU} = 2{,}500\text{ MIPS})$$
  - Result: $3{,}407$ raw readings ($1{,}703.5$ KB) were compressed into **$694$ batch tuples** ($451.1$ KB).

---

### C. Architecture D — Priority-Aware Fog Processing (`src/priority_aware/`)
- **Module:** `fog_priority_processor` hosted on `hospital_fog_gateway`.
- **Engine:** `PriorityProcessorEngine.java` + `PriorityFogDevice.java`.
- **Three-Tier Classification Logic:**

```text
Incoming Vital Reading (ReadingRecord)
  │
  ├── [CRITICAL Severity] (Heart Rate > 130 or < 45, SpO2 < 90%, Temp > 39°C, BP >= 160)
  │     ├── Path 1 (Emergency Bypass): Local Bedside Alert Tuple -> Bedside Actuator (7.1 ms)
  │     └── Path 2 (Background EHR): Urgent Notification Tuple -> Cloud Analytics (Uplink WAN)
  │
  ├── [ABNORMAL Severity] (Heart Rate 101-130, SpO2 90-94%, Temp 37.6-39°C, BP 130-159)
  │     └── Expedited Forwarding: Bypasses window W, sent directly to Cloud Analytics (208.5 ms)
  │
  └── [NORMAL Severity] (Routine physiological range)
        ├── Delta Redundancy Filter: If |Δ| < Threshold -> DROPPED AT FOG (712 readings dropped)
        └── Window Buffer: Accumulated up to W = 5 -> Dispatched as Aggregated Batch
```

---

## 4. Latency Distribution Analysis

The latency distribution across architectures illustrates the clinical trade-offs:

```
Latency Comparison by Severity Class (ms)

  Architecture A (Cloud-Only):
    Normal   : [████████████████████] 207.7 ms
    Abnormal : [████████████████████] 207.6 ms
    Critical : [████████████████████] 208.0 ms

  Architecture B (Fog Forwarding):
    Normal   : [█████████████████████] 211.4 ms
    Abnormal : [█████████████████████] 211.4 ms
    Critical : [█████████████████████] 211.7 ms

  Architecture C (Filtering & Aggregation W=5):
    Normal   : [████████████████████████████████████████████████████████████] 2,560.8 ms
    Abnormal : [████████████████████████████████████████████████████] 2,286.9 ms
    Critical : [████████████████████████████████████████████████████] 2,287.8 ms  <-- CLINICAL HAZARD

  Architecture D (Priority-Aware Fog):
    Normal   : [████████████████████████████████████████████████████████████████████] 3,169.2 ms
    Abnormal : [████████████████████] 208.5 ms (Expedited)
    Critical : [█] 7.1 ms (Emergency Bypass - 96.57% Faster!)  <-- CLINICAL EXCELLENCE
```

---

## 5. Bandwidth Conservation Analysis

### Theoretical vs. Empirical Network Traffic

$$\text{Cloud Bandwidth Saving (\%)} = \left(1 - \frac{\text{Traffic}_{\text{Arch}}}{\text{Traffic}_{\text{Arch A}}}\right) \times 100$$

- **Architecture A (Baseline):**
  $$\text{Cloud Traffic} = 4{,}000 \times 500\text{ Bytes} = 2{,}000{,}000\text{ Bytes} \implies 0.00\% \text{ Saving}$$
- **Architecture B (Forwarding):**
  $$\text{Cloud Traffic} = 4{,}000 \times 500\text{ Bytes} = 2{,}000{,}000\text{ Bytes} \implies 0.00\% \text{ Saving}$$
- **Architecture C (Filtering & Aggregation):**
  $$\text{Filtered Out} = 593 \text{ readings dropped at fog}$$
  $$\text{Batches Emitted} = 694 \text{ batches} \times 650\text{ Bytes} = 451{,}100\text{ Bytes}$$
  $$\text{Saving} = \left(1 - \frac{451{,}100}{2{,}000{,}000}\right) \times 100 = \mathbf{77.45\%}$$
- **Architecture D (Priority-Aware):**
  $$\text{Filtered Out} = 712 \text{ readings dropped at fog}$$
  $$\text{Batches Emitted} = 542 \text{ batches} \times 650\text{ Bytes} = 352{,}300\text{ Bytes}$$
  $$\text{Abnormal Expedited} = 398 \times 500\text{ Bytes} = 199{,}000\text{ Bytes}$$
  $$\text{Critical Background EHR} = 226 \times 500\text{ Bytes} = 113{,}000\text{ Bytes}$$
  $$\text{Total Cloud Traffic} = 352{,}300 + 199{,}000 + 113{,}000 = 664{,}300\text{ Bytes}$$
  $$\text{Saving} = \left(1 - \frac{664{,}300}{2{,}000{,}000}\right) \times 100 = \mathbf{66.79\%}$$

---

## 6. Generated Result Artifacts & Verification

All simulation outputs have been verified and exported to `results/`:

```text
results/
├── csv/
│   ├── arch_a_baseline_summary.csv        # 830 B (Cloud-only baseline summary metrics)
│   ├── arch_b_forwarding_summary.csv      # 825 B (Fog forwarding summary metrics)
│   ├── arch_c_filtering_summary.csv       # 846 B (Filtering & aggregation summary metrics)
│   └── arch_d_priority_summary.csv        # 847 B (Priority-aware fog summary metrics)
│
└── raw/
    ├── arch_a_baseline_readings.csv       # 398.8 KB (4,000 raw readings log)
    ├── arch_b_forwarding_readings.csv     # 398.8 KB (4,000 raw readings log)
    ├── arch_c_filtering_readings.csv      # 384.1 KB (4,000 raw readings with filtering tags)
    └── arch_d_priority_readings.csv       # 384.9 KB (4,000 raw readings with priority tags)
```

---

## 7. How to Reproduce All Results

To compile and execute all four architectures in sequence and display the automated comparison summary:

```powershell
# Execute full comparative simulation pipeline
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Arch ALL -NumPatients 10 -WindowSize 5

# Or run individual architectures
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Arch A
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Arch B
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Arch C -WindowSize 5
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Arch D -WindowSize 5
```

---

## 8. Viva Defense & Examination Guide

### Q1: Why does Architecture C have a higher average latency than Architecture A?
**Answer:** In Architecture A, each reading is transmitted to the cloud immediately upon creation. In Architecture C, the fog gateway intentionally buffers readings until a window of size $W=5$ is collected. Since readings are generated once every second ($1{,}000$ ms), accumulating $5$ readings incurs an inherent temporal buffering delay of approximately $2\text{--}4$ seconds. This is an intentional engineering trade-off: trading temporal latency for a **$77.45\%$ reduction in WAN bandwidth consumption**.

### Q2: Why is Architecture C unacceptable for clinical deployment without priority awareness?
**Answer:** In Architecture C, all vital signs are treated equally regardless of severity. When a patient experiences a cardiac crisis (e.g., Heart Rate > 130 BPM), the critical reading is queued in the aggregation buffer, taking **$2{,}287.762$ ms** to alert clinicians. In real-time acute care, multi-second alert delays increase patient mortality risk.

### Q3: How does Architecture D solve the safety hazard while retaining bandwidth savings?
**Answer:** Architecture D uses a multi-tier classification model at the fog gateway:
1. **Critical readings** bypass the aggregation buffer completely. A local bedside alert is dispatched within the hospital LAN in **$7.129$ ms** ($96.57\%$ faster than Cloud-Only), while an EHR log is sent to the cloud in parallel.
2. **Abnormal readings** are expedited immediately over WAN without aggregation wait ($208.5$ ms).
3. **Normal readings** ($84.4\%$ of total stream) undergo delta-filtering and window aggregation, ensuring the system still conserves **$66.79\%$ of cloud bandwidth**.

### Q4: How do you mathematically guarantee that results between architectures are comparable?
**Answer:** We used a deterministic pseudo-random number generator (`VitalSignGenerator`) initialized with a fixed seed (`42L`). Every architecture encounters the exact same patient streams, exact same physiological values, and exact same distribution of normal ($84.4\%$), abnormal ($10.0\%$), and critical ($5.7\%$) events at the exact same millisecond timestamps.

---

## 9. Next Steps (Transition to Phase 4)

With Phase 3 verified and complete, we are ready for **Phase 4 (Experiments, Parameter Sweeps & Scalability Analysis)** upon user approval:
1. **Scalability Experiment:** Scale patient load ($N = 10, 20, 30, 50$ patients, corresponding to $40, 80, 120, 200$ sensors) across all 4 architectures to analyze WAN bottlenecking and queuing latency.
2. **Aggregation Window Sweep:** Evaluate window sizes $W \in \{1, 3, 5, 10, 20\}$ in Architecture C and D to map the Pareto frontier of bandwidth conservation vs. buffering latency.
3. **Filtering Sensitivity Sweep:** Vary delta thresholds ($\delta \times 0.5, \delta \times 1.0, \delta \times 2.0$) to measure false-drop rates and filtering efficiency.
