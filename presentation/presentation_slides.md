# Topic 19: Fog-Assisted Real-Time Healthcare Monitoring System in iFogSim2
## 15-Minute Technical Defense & Conference Presentation Deck (19 Slides)

**Institution:** Department of Computer Science and Engineering, Indian Institute of Information Technology (IIIT) Sri City  
**Project:** Cloud Computing Research Assignment (Topic 19 — 15 Marks)  
**Authors:** Jayanth A, Mirthun V, Vinod K  
**Simulator:** iFogSim2 (Java 17 / CloudSim discrete-event engine)  
**Repository:** `https://github.com/jayanthananthakrishnan-a11y/Fog-Assisted-Real-Time-Healthcare.git`

---

<!-- SLIDE 1 -->
# Slide 1: Title & Team Credentials
### Topic 19: A Priority-Aware Fog-Assisted Framework for Real-Time Clinical Monitoring in Healthcare IoT

- **Course:** Cloud Computing (15-Mark University Research Project)
- **Team Members:**
  - **Jayanth A** (`jayanth.a22@iiits.in`)
  - **Mirthun V** (`mirthunvinayak.s22@iiits.in`)
  - **Vinod K** (`vinod.k22@iiits.in`)
- **Presentation Split (15 Minutes):**
  - **Speaker 1 (Jayanth A, 0:00 – 5:00):** Clinical Motivation, Latency Cliff Research Gap, and Architectures A through E (Slides 1–4).
  - **Speaker 2 (Mirthun V, 5:00 – 10:00):** Algorithmic Architecture: Sensor DAG, NEWS2 Risk Fusion, Greedy EDF Scheduling, TCP AIMD Window, and Complexity Profile (Slides 5–7).
  - **Speaker 3 (Vinod K, 10:00 – 15:00):** Empirical Results (Exp 1–9), Cluster Scaling, Fault-Tolerance, Security Roadmap, and Conclusions (Slides 8–19).

*Speaker Notes:*  
"Good morning, Professor and evaluation committee. Today our team presents Topic 19: Fog-Assisted Real-Time Healthcare Monitoring System in iFogSim2. Over the next 15 minutes, we will present how we built, benchmarked, and mathematically verified an intelligent edge computing architecture that cuts emergency alert delay by 96.6% while saving two-thirds of cloud bandwidth, eliminating 91.9% of spurious false alarms, and ensuring 100% SLA compliance during gateway failures."

---

<!-- SLIDE 2 -->
# Slide 2: Real-World Motivation & The Research Gap
### The Crisis of Unregulated Physiological Telemetry & The Latency Cliff

1. **Unbounded WAN Latency & Network Jitter:**
   - In Cloud-Only IoT healthcare, raw physiological tuples travel across the public WAN to remote cloud datacenters (RTT $\ge 200$ ms up to 1,000+ ms).
   - Clinical Emergency SLA: Acute cardiac events and hypoxemia require intervention in $\le 500$ ms.
2. **Excessive WAN Bandwidth Ingestion:**
   - Hospital wards capture 6+ vitals at 1 Hz. Over 85% of readings are clinically stationary resting telemetry.
3. **Alarm Fatigue from Motion Artifacts:**
   - Single-sensor transient noise spikes trigger false panic alarms, desensitizing medical staff.
4. **The Critical Research Gap (The Aggregation Latency Cliff):**
   - Conventional edge gateways batch readings into uniform First-In-First-Out (FIFO) windows ($W$) before WAN transmission.
   - Critical emergency tuples stall behind routine readings: delay explodes to **2,293.5 ms (W=5)** and **10.55 s (W=20)**, crashing SLA compliance to **4.62%**!
   - *Requirement:* Decouple emergency alert dispatching completely from aggregation windows.

*Speaker Notes:*  
"Today, hospital ICUs capture multi-parameter vital signs continuously. In conventional Cloud-Only deployments, streaming all raw readings over the WAN creates severe latency spikes that violate clinical SLAs, floods backhaul bandwidth, and triggers alarm fatigue. Crucially, prior edge systems batch readings into FIFO windows to save bandwidth, creating a lethal Latency Cliff where emergency alarms stall for seconds. Our framework directly solves this gap."

---
<!-- SLIDE 3 -->
# Slide 3: Proposed Solution: Priority-Aware Fog (Architecture D)
### Three-Tier Priority Bifurcation with Local Emergency Bypass

- **Data-Plane Bifurcation:** The Hospital Fog Gateway intercepts every sensor tuple and classifies it into three clinical tiers:
  1. **CRITICAL:** Instantly triggers local bedside actuator/monitor over local LAN in **7.13 ms** (96.57% latency reduction), completely bypassing WAN and aggregation queues. An asynchronous copy is sent to cloud EHR.
  2. **ABNORMAL:** Dispatched immediately across WAN to cloud analytics without window wait, ensuring prompt clinical review.
  3. **NORMAL:** Stateful delta-filtering ($\Delta_{\text{thresh}}$) suppresses redundant stationary readings; retained vitals are compressed into sliding windows (size $W$) to minimize cloud bandwidth.
- **Mathematical Bound:** $T_{\text{e2e}}^{\text{Crit}} = d_{\text{LAN}} + T_{\text{proc}}^{\text{fog}} + d_{\text{act}} = 2.0\text{ ms} + 0.025\text{ ms} + 1.0\text{ ms} \approx 7.13\text{ ms}$, completely immune to WAN delays.

*Speaker Notes:*  
"To solve this problem, we designed Architecture D. We split the data plane: critical emergency tuples bypass both the aggregation buffers and the WAN entirely, triggering local bedside alarms over the hospital LAN in 7.13 ms. Normal readings are delta-filtered and compressed, saving two-thirds of cloud bandwidth."

---

<!-- SLIDE 4 -->
# Slide 4: Architecture E: Multi-Ward Load Balancing & Failover
### Two-Gateway Inter-Ward Hospital Clustering

- **Dual-Ward Topology:** Models ICU Ward A and ICU Ward B, each managed by an 8,000 MIPS hospital fog gateway, interconnected via a **1.0 ms, 10 Gbps inter-ward hospital LAN**.
- **Heartbeat & Outage Detection:** Gateways exchange periodic keepalive heartbeats.
- **Seamless Inter-Ward Failover:**
  - If Ward A's gateway experiences a catastrophic hardware crash or queue saturation, Ward A's traffic reroutes across the inter-ward LAN to Ward B's gateway in **1.0 ms**.
  - Maintains **8.13 ms critical alert latency** during complete primary gateway failure.
  - Achieves **100.00% SLA compliance** and **100.00% failover success** with zero lost readings.
- **Cross-Sensor Artifact Suppression:** Embedded multi-vital correlation eliminates **91.9% of spurious motion spikes**.

*Speaker Notes:*  
"In Architecture E, we address hardware reliability by clustering two adjacent hospital wards over a 1.0 ms inter-ward LAN. If Ward A's gateway crashes, Ward B's gateway takes over immediately. During a 20-second complete gateway outage, critical alerts were delivered in 8.13 ms with 100% SLA compliance and zero dropped readings."

---

<!-- SLIDE 5 -->
# Slide 5: Sensor DAG Modeling & NEWS2 Composite Risk Fusion
### Formal Sensor-to-Actuator Telemetry Workflow

- **Clinical Sensor DAG $G = (V, E)$:**
  - $V = \{v_{\text{sense}}, v_{\Delta\text{-filter}}, v_{\text{fusion}}, v_{\text{sched}}, v_{\text{actuate}}, v_{\text{cloud}}\}$.
- **NEWS2 Multi-Vital Composite Fusion:**
  - Standardized clinical scoring protocol calculating $S_{\text{NEWS2}} \in [0, 20]$ across HR, $\text{SpO}_2$, BP, TEMP, RESP, and ECG HRV.
  - **Low Risk ($0 \le S \le 4$):** Eligible for delta-filtering and window aggregation.
  - **Medium Risk ($5 \le S \le 6$):** Direct WAN dispatch without buffering.
  - **High Risk / Emergency ($S \ge 7$):** Immediate local emergency bypass and queue preemption.
- **Cross-Sensor Motion Artifact Elimination:**
  - Isolated single-vital spikes with normal co-vitals are verified over 2 consecutive samples, eliminating 91.9% of false alarms without missing a single true crisis.

*Speaker Notes:*  
"We formalized our system as a Directed Acyclic Graph. At vertex v_fusion, we compute the standardized NEWS2 score from 0 to 20 across all six vitals. This clinical composite score allows the gateway to distinguish multi-organ decompensation from an isolated sensor motion artifact, eradicating ICU alarm fatigue."

---

<!-- SLIDE 6 -->
# Slide 6: Greedy EDF Scheduling & Adaptive TCP Windows (AIMD)
### Preemption and Personalized Dynamic Telemetry Compression

- **Greedy Earliest-Deadline-First (EDF) Scheduling:**
  - Three priority queues: $\mathcal{Q} = \{Q_{\text{crit}}, Q_{\text{abn}}, Q_{\text{norm}}\}$.
  - Deadlines: $\tau_{\text{crit}} = 500\text{ ms}$, $\tau_{\text{abn}} = 2000\text{ ms}$, $\tau_{\text{norm}} = 10000\text{ ms}$.
  - Strict preemption ensures critical tuples jump directly to service: $\pi^* = \arg\min \{D_i - t_{\text{current}}\}$.
- **TCP-Inspired AIMD Sliding Window Controller:**
  - **Additive Increase:** $W(t+1) = \min(W_{\max}, W(t) + 1)$ during stable vitals and queue utilization $< 70\%$.
  - **Multiplicative Decrease:** $W(t+1) = \max(1, \lfloor W(t)/2 \rfloor)$ upon abnormal vitals or queue congestion.
  - **Emergency Reset:** $W(t+1) = 1$ (immediate flush) during critical emergencies.
- **Three Evaluated Clinical Profiles:**
  - *Stable Post-Op (60%):* $W_{\max} = 20$ (79.6% bandwidth saving).
  - *Chronic Cardiac (25%):* $W_{\max} = 8$ (dynamic backoff on arrhythmia).
  - *Acute Unstable ICU (15%):* $W_{\max} = 2$ (continuous volatility forces immediate dispatch).

*Speaker Notes:*  
"To enforce deadlines, we implemented Greedy EDF scheduling with strict preemption across three queues. For window management, we adapted TCP AIMD congestion control across three distinct patient profiles, automatically expanding window size for stable patients while throttling for acute ICU cases."

---

<!-- SLIDE 7 -->
# Slide 7: Algorithmic Complexity & Empirical Edge Resource Profile
### Theoretical Bounds and Gateway Resource Consumption

| Algorithmic Module | Time Complexity | CPU Demand | Peak RAM | Energy per Tuple |
| :--- | :---: | :---: | :---: | :---: |
| Delta-Filter ($\Delta_{\text{thresh}}$) | $\mathcal{O}(1)$ | 0.05 MIPS | $< 1$ KB | 0.012 mJ |
| NEWS2 Composite Fusion | $\mathcal{O}(\|V\| + \|E\|)$ | 0.25 MIPS | $< 8$ KB | 0.065 mJ |
| Greedy Priority Queue | $\mathcal{O}(\log K)$ | 0.12 MIPS | $< 5$ KB | 0.031 mJ |
| AIMD Window Controller | $\mathcal{O}(1)$ | 0.08 MIPS | $< 2$ KB | 0.018 mJ |
| TLS 1.3 AES-GCM Framing | $\mathcal{O}(L)$ | 15.0 MIPS | 64 KB | 3.850 mJ |
| **Total Gateway Overhead** | **$\mathcal{O}(\log K)$** | **15.50 MIPS** | **12.4 MB (100 beds)** | **3.976 mJ** |

- **Gateway Capacity:** 8-core edge server with 8,000 MIPS processing power and 8,000 MB RAM.
- **Efficiency:** The entire priority engine consumes $< 0.2\%$ of gateway CPU and $< 13$ MB RAM for 100 concurrent beds.

*Speaker Notes:*  
"As shown in our complexity table, all our edge algorithms are ultra-lightweight. The entire priority processing suite consumes only 15.5 MIPS—less than 0.2% of our 8,000 MIPS gateway server—and requires under 13 MB of memory for 100 beds."

---

<!-- SLIDE 8 -->
# Slide 8: Simulation Rigor: Deterministic Setup in iFogSim2
### Experimental Reproducibility & Mathematical Accounting

- **iFogSim2 Simulation Core:** Built upon CloudSim discrete-event engine; customized with production clinical classes in Java 17.
- **Deterministic Seeding:** Fixed random seed (`seed = 42L`) ensures exact bit-level replay of physiological random walks and anomalies across all architectures.
- **Workload Calibration:** Bounded Gaussian walks calibrated against medical standards across HR, $\text{SpO}_2$, TEMP, BP, RESP, and HRV.
- **100.00% Exact Mathematical Tuple Accounting:**
  $$\text{Filtered} + \text{Artifacts Suppressed} + \text{Delivered} \equiv \text{Total Generated}$$
  Enforced by an explicit end-of-simulation buffer drain at $t = 100{,}500$ ms.

*Speaker Notes:*  
"To guarantee complete scientific rigor, all experiments were conducted in iFogSim2 with a fixed seed of 42. Exactly identical vital sign sequences were replayed across all architectures, with 100% mathematical accounting across all generated tuples."

---

<!-- SLIDE 9 -->
# Slide 9: Experiment 1: Baseline Architecture Comparison
### Latency by Severity and Cloud WAN Bandwidth (Figures 1 & 2)

- **Architecture A (Cloud-Only):** Uniform 207.71 ms latency across all severities; transmits 1,953.1 KB over WAN (0.0% savings).
- **Architecture B (Fog Forward):** Latency rises to 211.43 ms (+3.72 ms gateway penalty) with 0.0% bandwidth savings.
- **Architecture C (Filter & Agg):** Conserves 77.22% bandwidth, but critical latency explodes to **2,293.52 ms**, causing SLA compliance to crash to **27.88%**.
- **Architecture D (Priority Fog):** Cuts critical latency to **7.13 ms** (a 96.57% latency reduction), conserves **66.62% cloud bandwidth**, and achieves **100.00% SLA compliance**.

*Speaker Notes:*  
"Experiment 1 baseline results: Architecture A shows uniform 208 ms latency. Architecture C saves 77% bandwidth, but critical delay surges to 2,293 ms. Architecture D delivers critical alerts in 7.13 ms (96.6% faster) while saving 66.6% bandwidth with 100% SLA compliance."

---

<!-- SLIDE 10 -->
# Slide 10: Experiment 2: Patient & Sensor Scalability
### Scale-Invariance from 10 to 50 Patients (40 to 200 Sensors, Figures 3 & 4)

- **Scale-Invariance of Critical Response:**
  - Architecture D critical latency: 7.129 ms (40 sensors), 7.129 ms (80 sensors), 7.126 ms (120 sensors), and **7.125 ms (200 sensors)**.
  - Because alarms stay on the local switched LAN, scaling the hospital ward load introduces zero emergency latency degradation.
- **WAN Bandwidth Divergence:**
  - At 200 sensors, Architecture A floods the WAN with 9.77 MB of raw telemetry.
  - Architecture D transmits only 3.22 MB, saving **6.55 MB of WAN traffic (67.01%)**.

*Speaker Notes:*  
"Experiment 2 sweeps scale from 10 to 50 patients (40 to 200 sensors). In Architecture D, critical alert latency is completely scale-invariant: flat at 7.125 ms. Meanwhile, it saves 6.55 MB of WAN traffic at 200 sensors."

---

<!-- SLIDE 11 -->
# Slide 11: Experiment 3: Window Sensitivity & The Latency Cliff
### Pareto Trade-Off between Window Size and Emergency Delay (Figure 5)

- **The Latency Cliff in Architecture C:**
  - Increasing window size $W$ from 1 to 20 boosts cloud savings from 0% to 93.84%.
  - However, critical alert latency escalates from 209 ms to **10,551.19 ms (10.55 seconds)**!
  - Critical SLA compliance crashes from 100% down to a catastrophic **4.62%**.
- **Architecture D Decoupling:**
  - Our priority bypass completely decouples emergency safety from window size: critical latency remains pegged at **7.13 ms** with **100.00% SLA compliance** across all $W \in \{1, 2, 5, 10, 20\}$.
  - Allows hospitals to aggressively configure large windows ($W=20$) to save up to **79.64% cloud bandwidth** safely.

*Speaker Notes:*  
"Experiment 3 quantifies the latency cliff. In Architecture C, expanding window size pushes critical delay past 10 seconds, collapsing SLA compliance to 4.6%. In Architecture D, our local bypass decouples safety from window size, staying flat at 7.13 ms."

---

<!-- SLIDE 12 -->
# Slide 12: Experiment 4: WAN Stress & Network Fault-Tolerance
### Resilience under Severe Wide-Area Degradation (Figures 6 & 7)

- **Nominal Fiber (100 ms, 100 Mbps):** Arch A achieves 100% SLA compliance ($209.90$ ms $\le 500$ ms).
- **WAN Severe Bottleneck (400 ms, 250 Kbps):**
  - Architecture A latency surges to $855.31$ ms (**0.00% SLA compliance**).
  - Architecture C latency surges to $3{,}108.71$ ms (**0.00% SLA compliance**).
- **WAN Rural Degraded (600 ms, 100 Kbps):**
  - Architecture A latency reaches $1{,}317.17$ ms (**0.00% SLA compliance**).
- **Architecture D Robustness:**
  - Maintains **100.00% Critical SLA Compliance (433/433 alerts)** at a rock-solid **7.13 ms** across every degradation profile.

*Speaker Notes:*  
"Experiment 4 stress-tests WAN links down to 100 Kbps and 600 ms delay. Architectures A and C suffer complete failure (0% SLA). Architecture D maintains 100% SLA compliance at 7.13 ms because alarms stay on the local LAN."

---

<!-- SLIDE 13 -->
# Slide 13: Experiment 5: Multi-Ward Inter-Gateway Failover (Arch E)
### 20-Second Primary Gateway Outage Benchmark

- **Outage Scenario:** Ward A's primary fog gateway suffers a catastrophic crash from $t = 30$ s to $t = 50$ s (4,000 readings).
- **Architecture D (Single-Gateway):** Tuples stall in edge buffers; critical latency jumps to 163.63 ms, and SLA compliance drops to **68.20%**.
- **Architecture E (Two-Ward Failover):**
  - Inter-ward LAN keepalive triggers dynamic rerouting to Ward B's gateway in **1.0 ms**.
  - Ward A critical latency is maintained at **8.13 ms** (merely $+1.0$ ms over baseline).
  - Achieves **100.00% SLA compliance** and **100.00% failover success** with zero lost readings.
- **Cross-Sensor Artifact Suppression:** Eliminates **91.9% of false alarms** without suppressing a single true clinical crisis.

*Speaker Notes:*  
"Experiment 5 tests multi-ward failover during a 20-second gateway crash. While Architecture D degrades to 68% SLA, Architecture E achieves 100% failover success in 1.0 ms across the inter-ward LAN."

---

<!-- SLIDE 14 -->
# Slide 14: Experiment 6: Adaptive Sliding Window Dynamics (AIMD)
### Dynamic Telemetry Adaptation Across Patient Horizons (Figure 8)

- **Evaluated Across 6,480 Samples:**
  - 24-hour acute ICU horizon (high sampling volatility).
  - 30-day chronic ward horizon (long-term baseline drift).
- **Profile 1: Stable Post-Op (60%):** Stationarity allows window expansion up to $W = 20$, achieving **79.64% cloud bandwidth savings**.
- **Profile 2: Chronic Cardiac (25%):** Periodic arrhythmia episodes trigger multiplicative backoffs ($W \leftarrow \lfloor W/2 \rfloor$), bounding $W \in [4, 8]$.
- **Profile 3: Acute Unstable ICU (15%):** Constant threshold crossings force $W \to 1$, ensuring immediate zero-buffering forwarding.

*Speaker Notes:*  
"Experiment 6 evaluates our TCP AIMD window controller across 6,480 samples. Stable patients expand to W=20, cardiac patients back off to W=4-8, and ICU patients collapse to W=1 for instant alerts."

---

<!-- SLIDE 15 -->
# Slide 15: Experiment 7: Hop-by-Hop Latency & TLS 1.3 Overhead
### Latency Attribution and Empirical Cryptographic Profiling (Figures 9 & 10)

- **Four-Leg Latency Attribution:**
  - Leg 1 (Edge-to-Fog): $1.89$ ms.
  - Leg 2 (Fog-to-Cloud): $101.40$ ms.
  - Leg 3 (Cloud-to-Fog): $101.20$ ms.
  - Leg 4 (Fog-to-Edge Actuator): $1.02$ ms.
  - Architecture A round-trip: Legs $1+2+3+4 = 205.51$ ms.
  - Architecture D critical alert: Legs $1+4 = 2.91$ ms ($7.13$ ms with CPU processing).
- **TLS 1.3 Cryptographic Profiling:**
  - AES-GCM record framing adds $+37$ bytes/packet and $+15$ MIPS crypto CPU overhead.
  - Handshake latency: $+2.0$ ms LAN vs $+100.0$ ms WAN.
  - Critical SLA compliance remains strictly **100.00%**.
- **Root-Cause SLA Violation Breakdown:**
  - 72.1% of Architecture C violations are caused by window buffer capacity.
  - 0% of Architecture D violations stem from edge queueing.

*Speaker Notes:*  
"Experiment 7 profiles hop-by-hop latency and TLS 1.3 overhead. TLS 1.3 adds +37 bytes and +15 MIPS crypto CPU, but latency remains safely within 500 ms SLA. Furthermore, cross-sensor validation cuts false alarms by 91.9%."

---

<!-- SLIDE 16 -->
# Slide 16: Experiment 8: Fog Cluster Scalability (1 to 16 Gateways)
### Scaling to 500 Patients (2,000 Sensors, 200,000 Readings, Figure 10)

- **Single-Gateway Saturation:** Loaded with 100 beds, a single gateway reaches 84.2% CPU utilization and 92.0% SLA compliance.
- **Multi-Gateway Cluster (1 to 16 Nodes):** Throughput scales linearly up to **18,200 tuples/sec**.
- **16-Node Cluster at 500 Beds:**
  - Cluster CPU utilization stabilizes at a comfortable **28.4%**.
  - Peak RAM remains $< 16.2$ MB per gateway.
  - Critical emergency latency holds strictly at **7.15 ms** with **100.00% SLA compliance**.

*Speaker Notes:*  
"Experiment 8 scales the fog tier to 16 gateways serving 500 patients. At 16 nodes, CPU load is 28.4%, memory is under 17 MB, and emergency latency is rock-solid at 7.15 ms."

---

<!-- SLIDE 17 -->
# Slide 17: Experiment 9: Comparative Fault-Tolerance Benchmark
### Evaluation of Three Edge Fault-Tolerance Strategies (Figure 11)

- **Strategy 1 (Active-Passive Hot Standby):**
  - Heartbeat detection $150.0$ ms, recovery $25.0$ ms, outage latency $182.10$ ms, data loss $0.4\%$. Requires expensive dedicated standby hardware.
- **Strategy 2 (Active-Active Peer Offloading / Arch E):**
  - Queue threshold detection **12.0 ms**, recovery **1.0 ms**, outage latency **8.13 ms**, **0.0% data loss**. Optimal cost-efficiency.
- **Strategy 3 (Local Graceful Degraded Mode):**
  - Detection $50.0$ ms, recovery $0.0$ ms, outage latency **7.13 ms**, local flash caching during total WAN blackout.
- **Conclusion:** Active-Active Peer Offloading achieves sub-15 ms failover with zero idle hardware overhead.

*Speaker Notes:*  
"Experiment 9 compares three fault-tolerance strategies. Active-Active Peer Offloading (Strategy 2) achieves sub-15 ms failover and zero data loss without requiring idle standby hardware."

---

<!-- SLIDE 18 -->
# Slide 18: Future Security Roadmap: Ed25519, PQC & Blockchain
### Three-Pillar Security Architecture for Next-Gen Healthcare IoT

1. **Pillar 1: End-to-End Cryptographic Integrity & Non-Repudiation:**
   - Ed25519 digital signatures embedded on sensor nodes: $\sigma = \text{Sign}_{sk_{\text{sensor}}}(H(t_{\text{sample}} \,\|\, \text{patient\_id} \,\|\, \text{vitals}))$.
   - Fog verifies signatures in $\le 0.15$ ms, preventing biometric spoofing.
2. **Pillar 2: Post-Quantum Cryptography (PQC) Key Encapsulation:**
   - NIST FIPS 203 standard: **ML-KEM-768 (CRYSTALS-Kyber)** for quantum-resistant session key exchange between Fog and Cloud.
   - Public Key: 1,184 Bytes; Ciphertext: 1,088 Bytes; Encap/Decap: $< 0.08$ ms. Forward secrecy against future quantum adversaries.
3. **Pillar 3: Permissioned Blockchain EHR Audit Trails:**
   - Private Hyperledger Fabric ledger committing SHA-256 Merkle proofs of emergency overrides: $\text{BlockHash}_n = \text{SHA-256}(\dots)$.
   - Immutable, HIPAA-compliant audit logging for clinical accountability.

*Speaker Notes:*  
"In response to faculty feedback, we structured a complete Future Security Roadmap: Ed25519 signatures for provenance, NIST ML-KEM-768 for post-quantum key exchange, and permissioned blockchain for HIPAA-compliant EHR audit logging."

---

<!-- SLIDE 19 -->
# Slide 19: Conclusion & Deliverables
### Summary of University Research Contributions (15 Marks)

1. **Solved the Healthcare Latency Dilemma:** Cuts emergency latency by **96.57% (7.13 ms)** while conserving **66.62% to 79.64%** of cloud bandwidth.
2. **Clinical Rigor & Fault-Tolerance:** **91.9% false alarm reduction** via NEWS2 fusion, and **100.00% SLA compliance** during gateway outages via 1.0 ms LAN failover.
3. **Scalability & Security:** Verified cluster scaling across 16 gateways (500 beds) with empirical TLS 1.3 overhead profiling.
4. **Complete Submission Package:**
   - Full IEEE 7-page report (`report/Topic19_IEEE_Research_Report.docx` & `report/ieee_paper.tex`).
   - 11 publication-grade figures (300 DPI PNG + vector PDF).
   - 20-slide widescreen presentation deck (`presentation/Topic19_Healthcare_Fog_Presentation.pptx`).
   - Interactive 3D Hospital Ward visualizer (`dashboard/index.html`).
   - Deterministic, open-source Java simulation code in iFogSim2.

*Speaker Notes:*  
"To conclude: our priority-aware framework cuts critical latency by 96.6%, saves over 66% bandwidth, reduces false alarms by 91.9%, and guarantees 100% SLA compliance during outages. All artifacts are fully reproducible. Thank you, and we welcome your questions!"
