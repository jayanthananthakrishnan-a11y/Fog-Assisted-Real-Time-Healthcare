# A Priority-Aware Fog-Assisted Framework for Real-Time Clinical Monitoring in Healthcare IoT

**Jayanth Ananthakrishnan\*, Mirthunvinayak S\*, and Vinod Kumar\***  
*Department of Computer Science and Engineering, Indian Institute of Information Technology (IIIT) Sri City, Chittoor, Andhra Pradesh, India*  
*Emails: {jayanth.a22, mirthunvinayak.s22, vinod.k22}@iiits.in*  
*Topic 19: Fog-Assisted Real-Time Healthcare / Smart-City Monitoring System (iFogSim2)*

---

## Abstract
Continuous physiological telemetry in connected healthcare—monitoring heart rate, oxygen saturation ($\text{SpO}_2$), body temperature, blood pressure, respiration rate, and ECG HRV—generates massive streams of time-series biometric data. Transmitting raw biometric readings directly across Wide Area Networks (WAN) to centralized cloud datacenters incurs high transit latency, severe packet jitter, and excessive bandwidth consumption, jeopardizing patient safety during acute hemodynamic crises. While fog computing addresses WAN bottlenecks by relocating computation to local network gateways, conventional edge filtering and window-based aggregation inadvertently introduce a dangerous clinical safety hazard: time-critical physiological alarms queue behind routine biometric readings in First-In-First-Out (FIFO) aggregation buffers, creating a lethal **"latency cliff."**

In this paper, we propose, implement, and evaluate an end-to-end, priority-aware fog-assisted healthcare monitoring architecture modeled in **iFogSim2**. Our framework couples stateful delta-filtering and TCP-inspired adaptive sliding-window congestion control (AIMD) for routine vitals with a formal Sensor Directed Acyclic Graph (DAG) incorporating NEWS2 composite risk fusion and a preemptive Greedy Earliest-Deadline-First (EDF) priority scheduler. To ensure clinical fault-tolerance and hospital ward scalability, we implement **Architecture E (Multi-Ward Two-Gateway Load Balancing and Inter-Ward Failover)** with cross-sensor artifact suppression to eliminate ICU alarm fatigue. Across nine comprehensive experiments comprising over 200,000 physiological samples, 10 to 500 patients, and 1 to 16 distributed fog gateways, we contrast five paradigms: Cloud-Only Baseline (A), Fog Forwarding (B), Naive Fog Filtering and Aggregation (C), Priority-Aware Fog (D), and Multi-Ward Failover (E). Empirical results demonstrate that while naive edge aggregation (C) yields 77.22% cloud bandwidth savings, it imposes a 2,293.52 ms critical alert delay, causing clinical SLA ($\le 500$ ms) compliance to crash to 27.88%. In contrast, Architecture D and E decouple emergency responsiveness from window size $W$, achieving an emergency alert latency of **7.13 ms** (a 96.57% reduction over Cloud-Only), reducing false motion alarms by **91.9%**, maintaining **100.00% SLA compliance** during complete gateway outages via 1.0 ms inter-ward failover, and scaling reliably to 500 patients with empirical TLS 1.3 cryptographic overhead profiling. Finally, we formulate a concrete Future Security Roadmap detailing end-to-end ECDSA/Ed25519 digital signatures, Post-Quantum Cryptography (NIST FIPS 203 ML-KEM/Kyber), and permissioned blockchain EHR audit trails.

**Keywords:** Fog computing, Healthcare IoT, iFogSim2, real-time vital monitoring, delta-filtering, adaptive TCP sliding window, sensor DAG, NEWS2 risk fusion, greedy priority scheduling, inter-ward failover, TLS 1.3 overhead, quality of service (QoS).

---

## I. Introduction
The rapid proliferation of wearable and bedside Internet-of-Things (IoT) medical devices has catalyzed continuous physiological telemetry in intensive care units (ICUs), post-operative recovery wards, and ambulatory smart environments. Modern clinical protocols require simultaneous, multi-parameter vital sign monitoring, including Heart Rate (HR), Blood Oxygen Saturation ($\text{SpO}_2$), Core Body Temperature (TEMP), Systolic Blood Pressure (BP), Respiration Rate (RESP), and Electrocardiogram Heart Rate Variability (ECG HRV). When sampled at high frequencies across dozens of monitored beds, these biosensors emit relentless streams of discrete measurement tuples.

Historically, Internet of Medical Things (IoMT) systems have adopted a **Cloud-Only** architecture, wherein raw sensor tuples traverse local access bridges, hospital border routers, and multi-hop Wide Area Network (WAN) backhauls to reach hyper-scale cloud datacenters for centralized analytics, alerting, and Electronic Health Record (EHR) ingestion. However, routing high-frequency physiological streams exclusively to the cloud exposes three fundamental architectural limitations:
1. **WAN Latency and Network Jitter:** Uncontrolled wide-area round-trip times (RTT $\ge 200$ ms) and transit jitter prevent cloud analytics from honoring the strict sub-second response times ($\le 500$ ms) mandated by medical standards for acute hemodynamic crises.
2. **Bandwidth and Ingress Explosion:** Up to 85% of physiological readings captured from stable patients represent redundant, clinically stationary measurements. Streaming uncompressed raw telemetry continuously saturates WAN backhauls and incurs massive cloud storage ingestion overhead.
3. **Sensor Motion Artifacts and Alarm Fatigue:** Single-sensor transient voltage spikes or patient movement artifacts frequently trigger spurious clinical threshold crossings. In conventional cloud pipelines, every false threshold crossing generates panic alarms, contributing to severe clinical alarm fatigue.

To mitigate these drawbacks, **Fog Computing** has emerged as an intermediate tier that situates computation, storage, and networking resources at the edge of the local network. By performing data cleaning, duplicate suppression, and windowed aggregation at the fog tier, WAN traffic can be cut by more than 70%.

Nevertheless, a critical research gap persists in contemporary edge healthcare literature. Conventional edge aggregation architectures aggregate incoming telemetry tuples into sliding or tumbling batch windows ($W$) before WAN transmission. While effective for stationary signals, uniform FIFO window buffering introduces a devastating clinical side-effect: **The Aggregation Latency Cliff**. When an acute cardiac arrest or oxygen desaturation event occurs, the critical tuple is enqueued behind routine readings inside the edge buffer, remaining stalled until the entire window fills. Under typical telemetry sampling rates, this introduces multi-second delays, directly violating medical SLAs. Furthermore, edge nodes themselves are subject to hardware failures and queue saturation, demanding robust multi-ward failover strategies.

### Research Contributions
1. **Decoupled Multi-Tier Architecture (Architectures A–E):** We design and implement five comparative healthcare paradigms: Cloud-Only Baseline (A), Fog Forwarding (B), Naive Fog Filtering and Aggregation (C), Priority-Aware Fog (D), and Multi-Ward Two-Gateway Failover (E).
2. **Sensor DAG Modeling & NEWS2 Risk Fusion:** We formalize the sensor-to-actuator workflow as a clinical Directed Acyclic Graph (DAG) with National Early Warning Score 2 (NEWS2) multi-vital composite risk fusion (0–20 score), distinguishing isolated sensor motion artifacts from multi-organ decompensation.
3. **Greedy Earliest-Deadline-First (EDF) Priority Scheduling:** We implement a strict preemptive, three-tier priority scheduler enforcing hard clinical deadlines ($\tau_{\text{crit}}=500$ ms, $\tau_{\text{abn}}=2000$ ms, $\tau_{\text{norm}}=10000$ ms) that guarantees zero-wait local bypass for emergency alerts.
4. **TCP-Inspired Adaptive Sliding Window (AIMD):** We formulate an Additive Increase Multiplicative Decrease (AIMD) sliding-window controller that dynamically scales batch size $W$ based on patient physiological stability and fog queue utilization across 24-hour acute ICU and 30-day chronic ward horizons.
5. **Inter-Ward Failover & Extreme Fog Cluster Scalability:** We demonstrate 1.0 ms local LAN failover between adjacent hospital wards, preserving 100.00% SLA reliability during primary gateway failure, and evaluate cluster scaling up to 16 fog gateways and 500 patients (2,000 sensors) emitting 200,000 tuples.
6. **Hop-by-Hop Attribution & TLS 1.3 Overhead:** We provide empirical four-leg latency profiling and quantify TLS 1.3 AES-GCM record framing (+37 bytes), cryptographic CPU overhead (+15 MIPS), and handshake delays across LAN and WAN.
7. **Future Security Roadmap:** We formulate an actionable, mathematically grounded roadmap for ECDSA/Ed25519 digital signatures, NIST FIPS 203 ML-KEM (CRYSTALS-Kyber) post-quantum key encapsulation, and permissioned blockchain EHR audit trails.

---

## II. Related Work
Table 1 provides a comprehensive taxonomic comparison of our architecture against prominent literature in edge and fog healthcare monitoring.

| Publication | Environment | Vital Modalities | Delta-Filtering | Window Agg. | Priority Bypass | NEWS2 Fusion | Inter-Ward Failover | Strict SLA |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| Gupta et al. (2017) | iFogSim | Generic / EEG | No | No | No | No | No | No |
| Mahmud et al. (2022) | iFogSim2 | Generic Microservices | No | No | No | No | No | No |
| Rahmani et al. (2018) | UT-GATE Lab | ECG, Temp | Basic Threshold | No | Partial | No | No | No |
| Gia et al. (2015) | Hardware Testbed | ECG (QRS) | Feature Extraction | No | No | No | No | No |
| Kraemer et al. (2017) | Survey / Review | Multi-Parameter | Conceptual | No | Conceptual | No | No | No |
| Mutlag et al. (2019) | Survey / Taxonomy | Multi-Parameter | No | No | Conceptual | No | No | No |
| Verma & Sood (2018) | Simulation | Multi-Vital | Rule-based | No | No | No | No | No |
| Tuli et al. (2020) | FogBus / iFogSim | Cardiovascular | No | No | No | No | No | Partial |
| Pace et al. (2019) | BodyCloud | Generic BSN | Threshold | Yes ($W$) | No | No | No | No |
| Aazam & Huh (2014) | FiCloud Testbed | Generic IoT | Trimming | No | No | No | No | No |
| **Our Framework (Topic 19)** | **iFogSim2** | **HR, $\text{SpO}_2$, TEMP, BP, RESP, HRV** | **Stateful ($\Delta_{\text{thresh}}$)** | **Adaptive (AIMD)** | **Dedicated Local** | **Yes (0–20)** | **1.0 ms LAN (Arch E)** | **Strict ($\le$ 500 ms)** |

---

## III. System Architecture and Methodology

### A. Physical Topology and Network Modeling
The physical cyber-physical topology consists of three interconnected tiers:
1. **Patient IoT Sensor Tier:** Monitored patients $P_i \in \mathcal{P}$ equipped with biomedical sensors $\mathcal{S}_i = \{S_i^{\text{HR}}, S_i^{\text{SpO}_2}, S_i^{\text{TEMP}}, S_i^{\text{BP}}, S_i^{\text{RESP}}, S_i^{\text{HRV}}\}$. Sensors emit 500-byte tuples at $\Delta t_{\text{sample}} = 1{,}000$ ms.
2. **Hospital Ward Fog Gateway Tier:** Switched Ethernet/WLAN ($d_{\text{LAN}} = 2.0$ ms, $B_{\text{LAN}} = 10{,}000$ Mbps). Edge server provisioned with 8,000 MIPS and 8,000 MB RAM. Bedside actuators respond over $d_{\text{act}} = 1.0$ ms.
3. **Cloud Datacenter Tier:** Enterprise multi-processor cluster (44,000 MIPS, 40,000 MB RAM) connected via WAN ($d_{\text{WAN}} = 100$ ms, $B_{\text{WAN}} = 100$ Mbps).

### B. Architectural Taxonomy (Architectures A–E)
- **Architecture A (Cloud-Only Baseline):** 100% of raw readings sent directly to cloud across WAN. Round-trip alert traversal. No edge intelligence.
- **Architecture B (Fog Forwarding):** Passive gateway forwarder (200 MIPS). Demonstrates intermediate hop latency penalty with zero bandwidth conservation.
- **Architecture C (Naive Fog Filtering & Window Aggregation):** Delta-filtering with FIFO batch window ($W$). Critical alarms delayed inside aggregation queues.
- **Architecture D (Priority-Aware Fog with Local Emergency Bypass):** Data-plane bifurcation. Critical alarms trigger local bedside actuator in 7.13 ms. Asynchronous cloud copy. Normal vitals delta-filtered and batched.
- **Architecture E (Multi-Ward Two-Gateway Load Balancing and Failover):** Two adjacent wards connected via 1.0 ms inter-ward LAN. Incorporates cross-sensor artifact suppression. Heartbeat monitoring reroutes traffic to peer gateway upon primary outage.

### C. Clinical Sensor DAG Modeling and NEWS2 Risk Fusion
The telemetry workflow is modeled as a Directed Acyclic Graph $G = (V, E)$ with vertices $V = \{v_{\text{sense}}, v_{\Delta\text{-filter}}, v_{\text{fusion}}, v_{\text{sched}}, v_{\text{actuate}}, v_{\text{cloud}}\}$.

At vertex $v_{\text{fusion}}$, multi-vital telemetry is scored using the National Early Warning Score 2 (NEWS2) protocol:
$$S_{\text{NEWS2}} = \sum_{k \in \{\text{HR}, \text{SpO}_2, \text{BP}, \text{TEMP}, \text{RESP}, \text{HRV}\}} f_{\text{NEWS2}}(v_k)$$
where $S_{\text{NEWS2}} \in [0, 20]$. Scores are mapped to:
- **Low Risk ($0 \le S \le 4$):** Eligible for delta-filtering and window aggregation.
- **Medium Risk ($5 \le S \le 6$ or single-vital $= 3$):** Direct WAN dispatch without buffering.
- **High Risk / Emergency ($S \ge 7$):** Immediate local bedside bypass and queue preemption.

**Cross-Sensor Artifact Suppression:** Single-sensor threshold crossings are checked against simultaneous readings. Isolated single-sample spikes with normal co-vitals are verified on the subsequent sample, eliminating **91.9% of spurious motion artifacts**.

### D. Greedy Earliest-Deadline-First (EDF) Priority Scheduling
At vertex $v_{\text{sched}}$, three queues $\mathcal{Q} = \{Q_{\text{crit}}, Q_{\text{abn}}, Q_{\text{norm}}\}$ enforce clinical deadlines:
$$D_i = t_{\text{arr}} + \tau_{\text{tier}}, \quad \tau_{\text{crit}} = 500\text{ ms}, \; \tau_{\text{abn}} = 2000\text{ ms}, \; \tau_{\text{norm}} = 10000\text{ ms}$$
Strict preemptive priority guarantees $Q_{\text{crit}}$ dispatch before lower queues, scheduled via Greedy EDF:
$$\pi^* = \arg\min_{i \in Q_{\text{active}}} \{ D_i - t_{\text{current}} \}$$

### E. TCP-Inspired Adaptive Sliding Window (AIMD)
Window size evolves dynamically based on clinical stability and queue utilization:
$$W(t+1) = \begin{cases} \min(W_{\max}, W(t) + 1), & \text{if stable and } U_{\text{queue}} < 0.70 \\ \max(1, \lfloor W(t)/2 \rfloor), & \text{if abnormal or } U_{\text{queue}} \ge 0.70 \\ 1 \text{ (flush)}, & \text{if critical emergency} \end{cases}$$
Evaluated across three patient profiles:
1. **Stable Post-Op:** $W_{\max} = 20$, average window $\approx 16.4$, 79.64% bandwidth savings.
2. **Chronic Cardiac/COPD:** $W_{\max} = 8$, average window $\approx 5.8$, backoff on arrhythmia.
3. **Acute Unstable ICU:** $W_{\max} = 2$, frequent resets, average window $\approx 1.2$.

### F. Computational Complexity and Resource Profiling

| Module | Time Complexity | CPU Demand | Peak RAM | Energy/Tuple |
| :--- | :---: | :---: | :---: | :---: |
| Delta-Filter ($\Delta_{\text{thresh}}$) | $\mathcal{O}(1)$ | 0.05 MIPS | $< 1$ KB | 0.012 mJ |
| NEWS2 Fusion | $\mathcal{O}(\|V\| + \|E\|)$ | 0.25 MIPS | $< 8$ KB | 0.065 mJ |
| Greedy Priority Queue | $\mathcal{O}(\log K)$ | 0.12 MIPS | $< 5$ KB | 0.031 mJ |
| AIMD Window Controller | $\mathcal{O}(1)$ | 0.08 MIPS | $< 2$ KB | 0.018 mJ |
| TLS 1.3 AES-GCM Framing | $\mathcal{O}(L)$ | 15.0 MIPS | 64 KB | 3.850 mJ |
| **Total Gateway Overhead** | **$\mathcal{O}(\log K)$** | **15.50 MIPS** | **12.4 MB (100 beds)** | **3.976 mJ** |

---

## IV. Experimental Results and Discussion

### A. Experiments 1 & 2: Cross-Architecture Baseline & Scalability
- **Architecture A (Cloud-Only):** Uniform latency of 207.71 ms across all severities. 1,953.1 KB cloud bandwidth (0% savings).
- **Architecture B (Fog Forwarding):** Latency increases to 211.43 ms (+3.72 ms gateway inspection). 0% bandwidth savings.
- **Architecture C (Naive Aggregation):** 77.22% bandwidth savings, but critical alert latency explodes to **2,293.52 ms**, crashing SLA compliance to **27.88%**.
- **Architecture D (Priority Fog):** Critical latency drops to **7.13 ms** (96.57% faster than Cloud-Only), conserving 66.62% bandwidth with **100.00% SLA compliance**.
- **Scalability (10 to 50 Patients, 40 to 200 Sensors):** Architecture D critical latency remains perfectly flat at **7.125 ms** at 200 sensors, saving 6.55 MB of WAN traffic (67.01%).

### B. Experiments 3 & 4: Aggregation Latency Cliff & Network Stress
- In Architecture C, expanding $W$ from 1 to 20 increases bandwidth savings to 93.84%, but critical latency surges to **10,551.19 ms (10.55 s)** and SLA compliance drops to **4.62%**.
- Architecture D decouples critical response: critical latency remains pegged at **7.13 ms** with **100% SLA compliance** regardless of $W$.
- Under severe WAN bottlenecks (400 ms, 250 Kbps) and rural degradation (600 ms, 100 Kbps), Architecture A and C suffer **0.00% SLA compliance**. Architecture D maintains **100.00% SLA compliance** at 7.13 ms.

### C. Experiment 5: Multi-Ward Failover & Artifact Suppression
- Primary gateway outage (20 s): Under Architecture D (no backup), critical latency rises to 163.63 ms with 68.20% SLA compliance.
- Under Architecture E, inter-ward failover across 1.0 ms LAN preserves **8.13 ms critical latency**, achieving **100.00% SLA compliance** and **100.00% failover success**.
- Cross-sensor artifact suppression eliminates **91.9% of false alarms** while maintaining 100% sensitivity for true clinical crises.

### D. Experiment 6: Adaptive Sliding Window Dynamics (AIMD)
- Evaluated over 6,480 samples across 24h acute ICU and 30d chronic ward horizons (Figure 8).
- Stable Post-Op patients expand to $W=20$. Chronic Cardiac patients throttle to $W \in [4, 8]$. Acute ICU patients collapse to $W=1$.

### E. Experiment 7: Hop-by-Hop Latency Attribution & TLS 1.3 Overhead
- Four-leg latency breakdown: Edge-to-Fog (1.89 ms), Fog-to-Cloud (101.4 ms), Cloud-to-Fog (101.2 ms), Fog-to-Edge (1.02 ms).
- TLS 1.3 AES-GCM adds +37 bytes framing and +2.12 ms cryptographic delay, increasing traffic from 3,515.63 KB to 3,949.22 KB while maintaining 100% SLA compliance.
- Root-cause SLA analysis: 72.1% of Architecture C violations are caused by window buffer capacity. 0% of Architecture D violations stem from edge queueing.

### F. Experiment 8: Fog Cluster Scalability (1 to 16 Gateways, 50 to 500 Patients)
- 1 gateway with 100 patients experiences 84.2% CPU load and 92.0% SLA compliance.
- Scaling to 16 fog gateways across 500 patients (2,000 sensors, 200,000 readings) stabilizes CPU at 28.4%, peak RAM at 16.2 MB, and critical latency at **7.15 ms** with **100.00% SLA compliance**.

### G. Experiment 9: Comparative Fault-Tolerance Benchmark
- **Strategy 1 (Active-Passive Hot Standby):** Detection 150.0 ms, recovery 25.0 ms, outage latency 182.10 ms, data loss 0.4%.
- **Strategy 2 (Active-Active Peer Offloading / Arch E):** Detection 12.0 ms, recovery 1.0 ms, outage latency **8.13 ms**, **0.0% data loss**.
- **Strategy 3 (Local Graceful Degraded Mode):** Detection 50.0 ms, recovery 0.0 ms, outage latency **7.13 ms**, **0.0% data loss** (local flash caching).

### H. Experiment 10: Comparative Priority Fog Scheduling Policies
- Evaluated across 4 scheduling algorithms on 25,493 PhysioNet ICU tuples (`exp10_priority_scheduler_comparison.csv`):
  1. *FIFO (Arch C Baseline):* Critical p99 latency explodes to **9,840.50 ms**, SLA compliance collapses to **27.88%**.
  2. *Strict Static Priority:* Dispatches critical in 7.18 ms, but starves 428 normal readings (latency > 12.4 s), causing lower-tier SLA failure (96.20% compliance).
  3. *Weighted Fair Queuing (WFQ):* Zero starvation, but critical tail latency degrades to **84.10 ms** due to fair interleaving.
  4. *Preemptive Priority + Greedy EDF with Aging (Ours):* Delivers **7.13 ms mean / 8.45 ms p99 tail latency**, zero starvation, and **99.88% SLA compliance** (Figure 12a).

### I. Experiment 11: Multi-Objective Architecture Optimality & Pareto Scoring Matrix
- Standardized Composite Utility Score ($U_{\text{arch}} \in [0, 100]$) weighted across Latency (0.30), Bandwidth (0.20), SLA Availability (0.25), Energy (0.15), and MTTR Recovery (0.10) (`exp11_architecture_optimality_matrix.csv`):
  - Arch A (Cloud-Only): $54.11 / 100$
  - Arch B (Fog Forwarding): $52.86 / 100$
  - Arch C (Naive Filtering): $37.69 / 100$ (Worst rank due to the latency cliff)
  - Arch D (Priority Fog): $90.63 / 100$
  - **Arch E (Multi-Ward Failover): 98.34 / 100 (Rank #1)**

### J. Automated Patient Clinical Reporting
The automated clinical report generator generates per-patient diagnostic summaries (`results/patient_reports/patient_summary_report.json`), recording individual NEWS2 scores, suppressed artifacts, and realistic SLA compliance.

---

## V. Future Security Roadmap

### Pillar 1: End-to-End Cryptographic Integrity & Non-Repudiation
Embedded Ed25519 digital signatures on sensor nodes:
$$\sigma = \text{Sign}_{sk_{\text{sensor}}}(H(t_{\text{sample}} \,\|\, \text{patient\_id} \,\|\, \text{vitals}))$$
Fog gateway verifies signatures in $\le 0.15$ ms, preventing biometric spoofing.

### Pillar 2: Post-Quantum Cryptography (PQC) Key Encapsulation
Integration of NIST FIPS 203 standard: **ML-KEM-768 (CRYSTALS-Kyber)** for quantum-resistant session key exchange between Fog and Cloud:
- Public Key: 1,184 Bytes; Ciphertext: 1,088 Bytes; Encap/Decap: $< 0.08$ ms.
- Guarantees forward secrecy against future quantum adversaries.

### Pillar 3: Permissioned Blockchain EHR Audit Trails
Private Hyperledger Fabric ledger recording SHA-256 Merkle proofs of critical emergency overrides:
$$\text{BlockHash}_n = \text{SHA-256}\left(\text{BlockHash}_{n-1} \,\|\, \text{MerkleRoot}(\mathcal{M}_{\text{crit}}) \,\|\, \text{Timestamp}\right)$$
Immutable, HIPAA-compliant audit logging for clinical accountability.

---

## VI. Conclusion
In this paper, we resolved the tension between WAN bandwidth conservation, clinical emergency responsiveness, and edge fault-tolerance in IoT healthcare telemetry. Our proposed priority-aware fog framework (Architectures D and E) delivers critical alerts in **7.13 ms** (a 96.57% latency reduction over Cloud-Only), conserves **66.62% to 79.64%** cloud bandwidth via AIMD adaptive sliding windows, suppresses **91.9% of spurious motion artifacts**, preserves **100.00% SLA compliance** during primary gateway failure via 1.0 ms inter-ward failover, and scales linearly to 500 patients across 16 fog gateways.

---

## References
1. H. Gupta et al., "iFogSim: A toolkit for modeling and simulation of resource management techniques in IoT, Edge and Fog computing environments," *Softw. Pract. Exp.*, vol. 47, no. 9, pp. 1275–1296, 2017.
2. R. Mahmud et al., "iFogSim2: An Extended iFogSim Simulator for Mobility, Clustering, and Microservice Management in Edge and Fog Computing Environments," *J. Syst. Softw.*, vol. 190, p. 111351, 2022.
3. A. M. Rahmani et al., "Exploiting smart e-Health gateways at the edge of healthcare IoT: A fog computing approach," *Future Gener. Comput. Syst.*, vol. 78, pp. 641–658, 2018.
4. T. N. Gia et al., "Fog Computing in Healthcare Internet of Things: A Case Study on ECG Feature Extraction," in *Proc. IEEE CIT/IUCC/DASC/PICOM*, 2015, pp. 356–363.
5. F. A. Kraemer et al., "Fog Computing in Healthcare—A Review and Discussion," *IEEE Access*, vol. 5, pp. 9206–9222, 2017.
6. A. A. Mutlag et al., "Enabling technologies for fog computing in healthcare IoT systems: Future research directions," *IEEE Access*, vol. 7, pp. 81888–81922, 2019.
7. P. Verma and S. K. Sood, "Fog Assisted-IoT Enabled Patient Health Monitoring in Smart Homes," *IEEE Internet Things J.*, vol. 5, no. 3, pp. 1388–1396, 2018.
8. S. Tuli et al., "HealthFog: An ensemble deep learning based Smart Healthcare System for Automatic Diagnosis of Heart Diseases in Integrated IoT and Fog Computing Environments," *Future Gener. Comput. Syst.*, vol. 104, pp. 187–200, 2020.
9. P. Pace et al., "An edge-based architecture to support efficient applications for healthcare IoT," *IEEE Access*, vol. 7, pp. 43147–43159, 2019.
10. M. Aazam and E.-N. Huh, "Fog computing and smart gateway based communication for cloud of things," in *Proc. IEEE FiCloud*, 2014, pp. 464–470.
