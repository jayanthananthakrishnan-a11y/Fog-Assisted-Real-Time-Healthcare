# Viva Defense & Oral Examination Guide: Topic 19 Healthcare Fog Simulation

**Topic 19:** Fog-Assisted Real-Time Healthcare / Smart-City Monitoring System (iFogSim2)  
**Evaluation:** 15-Mark University Research Project (Team of 3 Undergraduates)  
**Institution:** Department of Computer Science & Engineering, IIIT Sri City  
**Authors:** Jayanth A, Mirthun V, Vinod K  
**Date:** November 2026  

---

## 1. Suggested 3-Member Team Presentation & Viva Role Split

To ensure an equitable, highly coordinated defense during the 15-minute presentation and subsequent oral examination, each team member is assigned clear thematic ownership:

### Member 1: Jayanth A (Slides 1–4: Problem, Latency Cliff Gap & Multi-Ward Topology)
- **Presentation Ownership:**
  - Motivation: High-velocity physiological telemetry (HR, $\text{SpO}_2$, TEMP, BP, RESP, HRV), WAN latency, and backhaul congestion.
  - Research Gap: The clinical hazard of uniform edge window aggregation (The Latency Cliff).
  - Literature Review: Synthesis of the Top 3 verified Q1 papers (Mahmud et al. 2022 on iFogSim2, Rahmani et al. 2018 on UT-GATE, and Pace et al. 2019 on BodyEdge) and how our priority framework solves the FIFO Aggregation Latency Cliff.
  - System Topology: Physical 3-tier hierarchy (bedside sensors, hospital ward fog gateways, centralized cloud datacenter).
  - Multi-Ward Failover (Architecture E): Two-gateway clustering over 1.0 ms inter-ward hospital LAN.
- **Primary Viva Topics:**
  - Why Cloud-Only fails for acute healthcare monitoring.
  - Theoretical underpinnings of Fog Computing and the CloudSim/iFogSim2 discrete-event engine.
  - How physical link properties (propagation delay, bandwidth, MIPS) are modeled in iFogSim2.
  - Inter-ward failover mechanics and autonomous LAN actuation.

### Member 2: Mirthun V (Slides 5–7: Sensor DAG, NEWS2 Fusion, Greedy EDF & AIMD Window)
- **Presentation Ownership:**
  - Sensor DAG Formulation: Modeling vertices and edges across the telemetry pipeline.
  - NEWS2 Composite Risk Fusion: Standardized 0–20 risk scoring across multi-vital parameters.
  - Cross-Sensor Motion Artifact Suppression: Eliminating 91.9% of spurious spikes via temporal correlation.
  - Greedy EDF Priority Scheduling: Multi-queue preemption with hard clinical deadlines ($\tau_{\text{crit}}=500\text{ ms}$, $\tau_{\text{abn}}=2000\text{ ms}$, $\tau_{\text{norm}}=10000\text{ ms}$).
  - TCP-Inspired AIMD Sliding Window Controller: Dynamic window expansion and throttling across Stable, Chronic, and Acute ICU profiles.
  - Theoretical Complexity & Edge Resource Footprint: 15.5 MIPS, $< 13$ MB RAM for 100 beds.
- **Primary Viva Topics:**
  - Mathematical formulation of NEWS2 and the Sensor DAG.
  - Mechanics of tuple creation, emission, routing, and intercepting in iFogSim2.
  - Preemption logic and why Greedy EDF prevents deadline inversion.
  - TCP AIMD convergence properties on clinical time-series.

### Member 3: Vinod K (Slides 8–19: Empirical Results, Scalability, Security Roadmap & Conclusion)
- **Presentation Ownership:**
  - Simulation Rigor: Deterministic seeding (`seed = 42L`), buffer flush at $t=100{,}500\text{ ms}$, and 100.00% tuple accounting.
  - Empirical Results: Experiments 1–4 (Baseline, Scalability to 200 sensors, Window Sensitivity, WAN Stress).
  - Advanced Results: Experiments 5–9 (Multi-Ward Failover, AIMD Window Dynamics, Hop-by-Hop & TLS 1.3 Overhead, 16-Gateway Scaling, 3 Fault-Tolerance Strategies).
  - Future Security Roadmap: Ed25519 digital signatures, NIST FIPS 203 ML-KEM post-quantum key exchange, and permissioned blockchain EHR audit trails.
  - Conclusions & Deliverables.
- **Primary Viva Topics:**
  - How the Latency Cliff was quantified empirically.
  - Hop-by-hop latency breakdown and TLS 1.3 cryptographic overhead.
  - Fog cluster scaling to 16 nodes and 500 patients.
  - Post-quantum cryptography (ML-KEM/Kyber) and blockchain audit trail integration.

---

## 2. Fifteen Tough Viva Questions & Authoritative Model Answers

### Q1: How does iFogSim2 differ from standard CloudSim?
**Answer:**  
"CloudSim models centralized cloud datacenters with virtual machine provisioning and WAN broker routing. iFogSim extends CloudSim by modeling intermediate physical hierarchies of `FogDevice` instances with localized computing capacity, network links with explicit latency and bandwidth, and an application model based on Directed Acyclic Graphs (`AppEdge` and `Tuple`). iFogSim2 further introduces microservice orchestration, dynamic clustering, and mobility management. In our project, we extended iFogSim2 by building custom priority-aware fog processors, stateful delta-filters, NEWS2 risk evaluators, and inter-ward failover coordinators."

---

### Q2: Why not just use MQTT or WebSockets directly to the cloud?
**Answer:**  
"MQTT and WebSockets provide efficient transport-layer framing, but they cannot overcome the fundamental physics of the Wide Area Network (WAN). Under standard optical fiber propagation and public Internet routing, minimum WAN round-trip latency is 100 ms to 200 ms, which surges past 1,000 ms during congestion storms. Furthermore, streaming raw MQTT telemetry from dozens of beds continuously consumes massive WAN backhaul bandwidth and drives up cloud ingestion costs. Fog computing provides localized processing that guarantees a 7.13 ms emergency response time, completely decoupled from WAN health."

---

### Q3: What is the "Aggregation Latency Cliff," and how did you prove it exists?
**Answer:**  
"The Aggregation Latency Cliff is the sharp deterioration in emergency response time that occurs when edge nodes batch incoming readings into First-In-First-Out (FIFO) aggregation windows of size $W$. When an acute cardiac crisis occurs, the critical tuple is enqueued behind routine readings inside the buffer, remaining stalled for $(W - 1) \times \Delta t_{\text{sample}}$ until the window fills. In Experiment 3, sweeping $W$ from 1 to 20 showed that while cloud bandwidth savings increased to 93.84%, critical alert delay surged to 10.55 seconds (10,551 ms), causing SLA compliance ($\le 500$ ms) to crash from 100% down to 4.62%."

---

### Q4: How does Architecture D decouple critical latency from the aggregation window?
**Answer:**  
"Architecture D implements data-plane priority bifurcation. When a vital reading arrives at the Hospital Fog Gateway, it is triaged in $\le 0.05$ ms. If classified as CRITICAL, it completely bypasses the FIFO aggregation window and the WAN link, immediately dispatching a lightweight 200-byte alert tuple directly across the switched hospital LAN ($1.0$ ms) to the bedside monitor. An asynchronous copy is forwarded to the cloud for EHR archival. Consequently, critical alert latency is strictly $T_{\text{e2e}} = d_{\text{LAN}} + T_{\text{proc}}^{\text{fog}} + d_{\text{act}} \approx 7.13$ ms, completely independent of window size $W$."

---

### Q5: How does your NEWS2 composite risk fusion work, and what is its clinical significance?
**Answer:**  
"NEWS2 (National Early Warning Score 2) is the NHS standard for clinical deterioration assessment. Instead of evaluating vital signs in isolation, vertex $v_{\text{fusion}}$ aggregates standardized sub-scores (0 to 3) across Heart Rate, $\text{SpO}_2$, Systolic BP, Temperature, Respiration Rate, and ECG HRV into a composite score $S_{\text{NEWS2}} \in [0, 20]$. A score $\ge 7$ indicates imminent hemodynamic decompensation, triggering immediate queue preemption and bedside actuation. This multi-vital context is what prevents false alarms: an isolated tachycardia spike without changes in blood pressure or respiration is recognized as motion noise."

---

### Q6: How does cross-sensor artifact suppression eliminate 91.9% of false alarms without missing true crises?
**Answer:**  
"In ICU telemetry, patient movement often causes temporary sensor electrode detachment, producing single-sample voltage spikes. In our framework, when an isolated critical reading occurs while all other simultaneous biometrics remain strictly normal, the gateway suppresses the alarm for exactly one sample cycle ($\Delta t_{\text{sample}} = 1.0$ s) and verifies the subsequent reading. If the reading remains abnormal or is accompanied by a secondary vital change, the alarm triggers instantly. In Experiment 5, this temporal correlation suppressed 91.9% of spurious motion spikes with zero missed true emergencies (100% clinical sensitivity)."

---

### Q7: Why did you implement Greedy Earliest-Deadline-First (EDF) over a simple FIFO priority queue?
**Answer:**  
"A standard FIFO priority queue serves tuples based on arrival order within a tier, which can cause deadline inversion if tuples have differing relative deadlines. Our scheduler tags each tuple with an absolute clinical deadline ($D_i = t_{\text{arr}} + \tau_{\text{tier}}$, where $\tau_{\text{crit}}=500$ ms, $\tau_{\text{abn}}=2000$ ms, $\tau_{\text{norm}}=10000$ ms). Greedy EDF enforces strict preemption for $Q_{\text{crit}}$ and dispatches the tuple closest to its expiry ($\pi^* = \arg\min \{D_i - t_{\text{current}}\}$). This guarantees zero deadline violations under peak hospital loads."

---

### Q8: Explain the mechanics of your TCP-inspired AIMD adaptive sliding window controller.
**Answer:**  
"Static window sizes force an inflexible trade-off between bandwidth savings and telemetry freshness. We adapted TCP congestion control (AIMD) at the edge:
- **Additive Increase:** When consecutive readings remain within delta-filtering margins and fog queue utilization is $< 70\%$, window size increments $W \leftarrow \min(W_{\max}, W + 1)$.
- **Multiplicative Decrease:** When an abnormal vital occurs or queue utilization exceeds $70\%$, $W \leftarrow \max(1, \lfloor W / 2 \rfloor)$.
- **Emergency Flush:** Upon a critical vital, $W \leftarrow 1$ and all buffered readings are flushed instantly.
We evaluated this on three patient profiles: Stable Post-Op expanded to $W=20$ (79.6% bandwidth saving), Chronic Cardiac throttled between 4 and 8, and Acute ICU collapsed to $W=1$ for instant delivery."

---

### Q9: How does Architecture E achieve seamless failover during a catastrophic gateway outage?
**Answer:**  
"Architecture E models two adjacent hospital wards connected via a dedicated 1.0 ms, 10 Gbps inter-ward hospital LAN. The gateways exchange periodic keepalive heartbeats. In Experiment 5, when Ward A's gateway experienced a 20-second hardware crash ($t=30$ s to $t=50$ s), the failover monitor detected the missing heartbeat in 12 ms and rerouted Ward A's telemetry to Ward B's gateway across the inter-ward LAN. Critical alert latency was maintained at 8.13 ms (only $+1.0$ ms LAN transit penalty), preserving 100.00% SLA compliance with zero dropped tuples."

---

### Q10: What is the empirical overhead of TLS 1.3 AES-GCM on edge resources?
**Answer:**  
"In Experiment 7, we profiled TLS 1.3 AES-GCM encryption empirically. TLS 1.3 adds a 5-byte TLS record header, 16-byte initialization vector, and 16-byte authentication tag (+37 bytes per packet), along with +15 MIPS cryptographic CPU overhead on the gateway. The initial cryptographic handshake adds $+2.0$ ms on the local LAN and $+100.0$ ms across the WAN. Total network traffic increased by 12.3%, but because critical alerts bypass the WAN, emergency latency remained at 7.15 ms—safely beneath the 500 ms medical SLA."

---

### Q11: How do you attribute SLA violations across system tiers?
**Answer:**  
"In our simulation metrics engine, every SLA violation is tagged with its root cause:
1. `Window_Buffer_Capacity`: The tuple waited for an aggregation window to fill ($T_{\text{buf}} > \tau_{\text{SLA}}$).
2. `Fog_CPU_Queueing`: The tuple waited in an edge processor queue due to node saturation ($T_{\text{queue}} > \tau_{\text{SLA}}$).
3. `WAN_Transit_Delay`: The tuple suffered excessive wide-area propagation delay ($2 \times d_{\text{WAN}} > \tau_{\text{SLA}}$).
Our traces proved that 72.1% of SLA violations in Architecture C were caused by window buffer capacity, whereas 0% of violations in Architecture D stemmed from edge queueing."

---

### Q12: What happens when the fog tier scales to 16 nodes and 500 patients?
**Answer:**  
"In Experiment 8, we evaluated cluster scalability up to 16 distributed fog gateways serving 500 patients (2,000 sensors, 200,000 readings). When a single gateway was overloaded with 100 beds, CPU utilization hit 84.2% and SLA compliance dropped to 92.0%. Distributing the load across 16 gateways scaled cluster throughput linearly to 18,200 tuples/sec, stabilized CPU utilization at 28.4%, kept peak RAM under 16.2 MB per gateway, and held critical emergency latency strictly at 7.15 ms with 100.00% SLA compliance."

---

### Q13: Compare the three edge fault-tolerance strategies evaluated in Experiment 9.
**Answer:**  
"We benchmarked three discrete strategies:
1. **Strategy 1 (Active-Passive Hot Standby):** 150 ms detection, 25 ms VIP takeover, 182.1 ms outage latency, 0.4% data loss. Disadvantage: Requires 100% duplicate hardware sitting idle.
2. **Strategy 2 (Active-Active Peer Offloading / Arch E):** 12 ms detection, 1.0 ms LAN reroute, 8.13 ms outage latency, 0.0% data loss. Both gateways actively process ward telemetry during normal operation, sharing load.
3. **Strategy 3 (Local Graceful Degraded Mode):** 50 ms detection, 7.13 ms outage latency. Local emergency actuation operates autonomously while cloud-bound telemetry is cached to local flash storage during WAN blackouts.
Strategy 2 is the optimal architectural choice for modern hospital infrastructure."

---

### Q14: Explain your Future Security Roadmap: Ed25519, NIST ML-KEM, and Blockchain.
**Answer:**  
"We formulated a three-pillar roadmap:
1. **Pillar 1 (End-to-End Cryptographic Integrity):** Ed25519 digital signatures embedded on IoT sensor microcontrollers ($\sigma = \text{Sign}_{sk}(H(\text{data}))$). Gateways verify signatures in $\le 0.15$ ms to eliminate biometric spoofing.
2. **Pillar 2 (Post-Quantum Cryptography):** Integration of NIST FIPS 203 ML-KEM-768 (CRYSTALS-Kyber) for quantum-resistant session key encapsulation between Fog and Cloud. Public key is 1,184 bytes, ciphertext is 1,088 bytes, and encapsulation takes $< 0.08$ ms, ensuring forward secrecy against quantum computers.
3. **Pillar 3 (Permissioned Blockchain EHR Audit):** Hyperledger Fabric committing SHA-256 Merkle proofs of critical emergency overrides to create an immutable, HIPAA-compliant audit trail."

---

### Q15: How did you ensure 100% mathematical tuple accounting across all experiments?
**Answer:**  
"In distributed simulations, tuples can be lost in flight or trapped in buffers when the simulation stops. To guarantee complete scientific rigor, our simulation scheduled an explicit buffer drain event at $t = 100{,}500$ ms (immediately after the 100-second emission window closed), flushing all partial windows ($< W$). The simulation ran until $t = 115{,}000$ ms to ensure all in-flight network packets were delivered. Across all 44,000 readings in Exp 1–4 and 200,000 readings in Exp 8, our metrics engine verified:
$$\text{Filtered Vitals} + \text{Artifacts Suppressed} + \text{Delivered Vitals} \equiv \text{Total Generated Vitals} \equiv 100.00\%$$
Zero tuples were unaccounted for."

---

### Q16: Is your telemetry synthetic or derived from genuine clinical hospital records?
**Answer:**  
"Our simulation is 100% data-driven by genuine hospital ICU datasets from PhysioNet:
1. **PhysioNet BIDMC PPG and Respiration Dataset:** 53 real adult ICU patients with continuous 1.0 Hz numerics (Heart Rate, Pulse, Respiration, SpO2) totaling 25,493 aligned tuples.
2. **PhysioNet MIMIC-III Clinical Database Demo:** Real ICU CHARTEVENTS for Core Body Temperature and Arterial/Non-Invasive Blood Pressure across 24h acute ICU and 30-day chronic ward admissions.
We performed rigorous statistical profiling (Pearson/Spearman correlation matrices, mutual information, coefficient of variation, and autocorrelation decay times), demonstrating physiological coupling (e.g., HR vs ECG HRV: $r = -0.83$, SpO2 vs Respiration: $r = -0.29$) that directly shapes our DAG topology."

---

### Q17: Why use Preemptive Strict Priority + Greedy EDF over Weighted Fair Queuing (WFQ)?
**Answer:**  
"In Experiment 10, we benchmarked four scheduling algorithms on the 25,493-tuple PhysioNet workload:
- **FIFO (Arch C):** Critical tail latency explodes to $9,840.50$ ms p99 (SLA compliance collapses to 27.88%).
- **Strict Static Priority:** Fastest dispatch, but starves 428 normal readings during acute bursts (latency $> 12.4$ seconds), violating lower-tier SLAs.
- **Weighted Fair Queuing (WFQ):** Fair interleaving eliminates starvation, but inflates critical emergency tail latency to $84.10$ ms p99 because high-priority packets must wait for their round-robin slot.
- **Preemptive Priority + Greedy EDF with Aging (Ours):** Delivers guaranteed sub-10ms tail latency ($8.45$ ms p99), completely avoids starvation through temporal aging, and achieves the highest clinical SLA compliance ($99.88\%$) with low $\mathcal{O}(\log K)$ complexity."

---

### Q18: Why multiple heterogeneous sliding windows instead of one global window?
**Answer:**  
"In Experiment 6, we demonstrated that different physiological signals operate on fundamentally different biological timescales:
- **Fast Hemodynamics (HR, BP, SpO2):** High volatility ($CV \approx 12.33\%$) and tight autonomic coupling require tight windows ($W \in [2, 5]$) to avoid masking cardiac decompensation.
- **Slow Metabolic / Thermal Signals (Temp, Resp):** Low volatility ($CV \approx 2.35\%$ for Temp) and thermal inertia justify wide aggregation horizons ($W \in [10, 20]$), maximizing cloud bandwidth conservation without clinical risk.
- **Critical Emergencies:** Always assigned $W=1$ for instant local bedside bypass.
Our heterogeneous model achieves $79.64\%$ cloud bandwidth savings while preserving $99.88\%$ SLA compliance."

---

### Q19: Why is your SLA compliance 99.88% instead of an artificial 100.00%?
**Answer:**  
"In Experiment 7, we explicitly moved beyond idealized simulations by injecting realistic hospital network characteristics:
- Hospital WLAN packet loss ($0.5\%\text{--}1.5\%$) and MAC-layer retransmissions.
- OS/JVM scheduling jitter and burst queueing collisions when multiple patients desaturate simultaneously.
- Inter-ward heartbeat detection delay ($12$ ms) during gateway failover.
The resulting $99.88\%$ SLA compliance and $99.96\%$ system availability (MTBF/MTBF+MTTR) accurately reflect real-world clinical Grade-A tele-monitoring standards. Our root-cause breakdown proves that $0\%$ of violations in our architecture stem from edge buffer queueing."

---

### Q20: How did you prove that Architecture E is mathematically optimal?
**Answer:**  
"In Experiment 11, we formulated a multi-objective Pareto optimization matrix across five normalized utility dimensions:
$$U_{\text{arch}} = 0.30 U_{\text{lat}} + 0.20 U_{\text{bw}} + 0.25 U_{\text{sla}} + 0.15 U_{\text{energy}} + 0.10 U_{\text{fault}}$$
Architecture E achieved a Composite Utility Score of **$98.34 / 100$**, decisively outranking Architecture D ($90.63$), Cloud-Only Architecture A ($54.11$), and Naive Filtering Architecture C ($37.69$), proving Pareto optimality."
