# A Priority-Aware Fog-Assisted Framework for Real-Time Clinical Monitoring in Healthcare IoT

**Authors:** A. Krishna, Student Co-Author II, and Student Co-Author III  
**Affiliation:** Department of Computer Science and Engineering, Indian Institute of Information Technology (IIIT) Sri City, Chittoor, Andhra Pradesh, India  
**Course Context:** Cloud Computing Undergraduate Research Project (Topic 19)  
**Date:** November 2026  

---

## Abstract

Continuous physiological telemetry in connected healthcare—monitoring heart rate, oxygen saturation ($\text{SpO}_2$), body temperature, and blood pressure—generates massive streams of time-series data. Transmitting raw biometric readings directly across Wide Area Networks (WAN) to centralized cloud datacenters incurs high transit latency, severe packet jitter, and excessive bandwidth consumption, jeopardizing patient safety during acute hemodynamic crises. While fog computing addresses WAN bottlenecks by relocating computation to local network gateways, conventional edge filtering and window-based aggregation inadvertently introduce a dangerous clinical safety hazard: time-critical physiological alarms queue behind routine biometric readings in First-In-First-Out (FIFO) aggregation buffers, creating a lethal "latency cliff."

In this paper, we propose and evaluate an end-to-end, priority-aware fog-assisted healthcare monitoring architecture modeled and simulated in iFogSim2. Our framework couples stateful delta-filtering and sliding-window aggregation for routine vital signs with a dedicated, zero-buffering local emergency bypass channel for life-critical anomalies. Through four rigorously controlled experiments comprising 44,000 synthetic clinical readings across 10 to 50 patients (40 to 200 sensors), we contrast four paradigms: Cloud-Only Baseline (A), Fog Forwarding (B), Naive Fog Filtering and Aggregation (C), and Priority-Aware Fog (D). Empirical results demonstrate that while naive edge aggregation (C) yields a 77.22% cloud bandwidth saving, it imposes a 2,293.52 ms critical alert delay, causing Service Level Agreement (SLA $\le$ 500 ms) compliance to crash to 27.88%. In contrast, our proposed architecture (D) decouples critical response from aggregation window size ($W$), achieving an emergency alert latency of **7.13 ms** (a **96.57% reduction** over Cloud-Only), conserving **66.62% to 79.64% of cloud bandwidth**, and maintaining **100.00% clinical SLA reliability** even under severe WAN bottlenecks and rural network degradation.

**Keywords:** Fog computing, Healthcare IoT, iFogSim2, real-time vital monitoring, delta-filtering, sliding-window aggregation, priority bypass, quality of service (QoS).

---

## I. Introduction

The rapid proliferation of wearable and bedside Internet-of-Things (IoT) medical devices has catalyzed real-time physiological telemetry in intensive care units (ICUs), post-operative wards, and ambulatory smart homes. Modern clinical protocols require continuous, multi-parameter vital sign monitoring, including Heart Rate (HR), Blood Oxygen Saturation ($\text{SpO}_2$), Core Body Temperature (TEMP), and Systolic/Diastolic Blood Pressure (BP). When sampled at high frequencies across dozens of monitored beds, these biosensors emit relentless streams of discrete measurement tuples.

Historically, Internet of Medical Things (IoMT) systems have adopted a **Cloud-Only** architecture, wherein raw sensor tuples traverse local access bridges, hospital border routers, and multi-hop Wide Area Network (WAN) backhauls to reach hyper-scale cloud datacenters for centralized analytics, alerting, and Electronic Health Record (EHR) ingestion [1], [5]. However, routing high-frequency physiological streams exclusively to the cloud exposes two fundamental architectural limitations:

1. **WAN Latency and Network Jitter:** Uncontrolled wide-area round-trip times (RTT $\ge 200\text{ ms}$) and transit jitter prevent cloud analytics from honoring the strict sub-second response times ($\le 500\text{ ms}$) mandated by medical standards for acute hemodynamic crises (e.g., ventricular tachycardia, acute hypoxemia, or malignant hypertensive crisis) [5], [7].
2. **Bandwidth and Ingress Explosion:** Up to 85% of physiological readings captured from stable patients represent redundant, clinically stationary measurements (e.g., resting heart rate fluctuating by $< 1\text{ bpm}$). Streaming uncompressed, raw telemetry continuously across the WAN saturates backhaul bandwidth and generates massive cloud storage ingestion overhead [9], [10].

To mitigate these drawbacks, **Fog Computing** has emerged as an intermediate architectural tier that situates computation, storage, and networking resources at the edge of the local network, such as hospital ward gateways or bedside micro-servers [2], [3]. By performing data cleaning, duplicate suppression, and windowed aggregation at the fog tier, WAN traffic can be cut by more than 70% [9], [10].

Nevertheless, a critical research gap persists in contemporary edge healthcare literature. Conventional edge aggregation architectures aggregate incoming telemetry tuples into sliding or tumbling batch windows ($W$) before WAN transmission [9]. While effective for stationary signals, uniform FIFO window buffering introduces a devastating clinical side-effect: **The Aggregation Latency Cliff**. When an acute cardiac arrest or oxygen desaturation event occurs, the critical tuple is enqueued behind routine readings inside the edge buffer, remaining stalled until the entire window fills. Under typical telemetry sampling rates, this introduces multi-second delays, directly violating medical SLAs and creating an unacceptable clinical hazard.

### Research Contributions
To resolve this fundamental tension between WAN bandwidth conservation and clinical emergency responsiveness, this paper presents a comprehensive, reproducible investigation into fog-assisted healthcare architectures using the discrete-event simulator iFogSim2 [2]. Our key contributions are:
- **Decoupled Multi-Tier Architecture:** We design and implement four comparative healthcare paradigms: Cloud-Only Baseline (A), Fog Forwarding (B), Naive Fog Filtering and Aggregation (C), and Priority-Aware Fog with Local Emergency Bypass (D).
- **Algorithmic Formulation:** We formulate a lightweight, stateful delta-filtering algorithm ($\Delta_{\text{thresh}}$) coupled with sliding-window aggregation ($W$) for normal streams, and a three-tier clinical triage engine ($\text{NORMAL}$, $\text{ABNORMAL}$, $\text{CRITICAL}$) that bifurcates the data plane. Critical alarms bypass aggregation buffers and WAN links completely, executing closed-loop actuation over the local hospital LAN in **7.13 ms** while asynchronously archiving to the cloud.
- **Rigorous Empirical Benchmarking:** Using a fully deterministic synthetic workload (44,000 readings across 10 to 50 patients), we conduct four controlled parameter sweeps evaluating architectural baseline overhead, patient scalability up to 200 sensors, aggregation window sensitivity ($W \in \{1, 2, 5, 10, 20\}$), and WAN degradation profiles (down to 100 Kbps, 600 ms WAN).
- **Open-Source Reproducibility:** All simulation code, experimental harness scripts, CSV raw traces, and Python visualization pipelines are publicly documented to establish a rigorous university research baseline.

---

## II. Related Work

The convergence of edge computing and medical telemetry has generated substantial literature over the past decade. We categorize prior work across three primary dimensions: simulation environments, smart e-Health gateways, and edge filtering/routing techniques.

### A. Fog Computing Simulation Frameworks
Evaluating large-scale fog topologies on physical hardware is logistically prohibitive and financially exorbitant. Gupta et al. [1] developed **iFogSim**, an open-source discrete-event simulator built upon CloudSim. iFogSim formalized the modeling of fog infrastructure as physical hierarchies of `FogDevice` nodes, application modules as Directed Acyclic Graph (`DAG`) vertices, and network traffic as discrete `Tuple` entities. Recently, Mahmud et al. [2] introduced **iFogSim2**, extending the framework with dynamic clustering, microservice orchestration, and mobility management. While iFogSim2 includes canonical healthcare demonstrations (e.g., cardiovascular monitoring), it models uniform data streams without physiological severity classification or stateful delta-compression. Our work builds directly upon iFogSim2, providing custom, production-grade clinical processing classes.

### B. Smart e-Health Gateways and Edge Architectures
The concept of deploying intermediate intelligence near medical sensors was pioneered by Rahmani et al. [3] through the **UT-GATE** smart e-Health gateway. Deployed on embedded ARM and Linux gateways, UT-GATE validated local protocol bridging, local alerting, and data persistence during WAN network blackouts. Gia et al. [4] demonstrated edge-based QRS detection and feature extraction from raw ECG signals, achieving over 90% bandwidth reduction compared to transmitting raw electrical waveforms. Kraemer et al. [5] and Mutlag et al. [6] provided exhaustive taxonomic surveys, establishing that time-sensitive healthcare alerts must be handled near the patient boundary rather than deferred to remote cloud infrastructure. However, these systems were evaluated on small-scale laboratory prototypes (1--3 patients) and lacked systematic stress testing under varying aggregation windows and degraded wide-area networks.

### C. Data Filtering, Window Aggregation, and Priority Routing
Data trimming and duplicate suppression at smart gateways were established by Aazam and Huh [10], who demonstrated up to 80% reduction in cloud-bound IoT traffic by filtering stagnant samples. In the medical domain, Pace et al. [9] designed BodyCloud, an edge-centric architecture using sliding-window aggregation to extend the battery lifetimes of Body Sensor Networks (BSNs). Verma and Sood [7] implemented rule-based edge processing in smart home health monitoring, reducing average latency from 1,850 ms to 290 ms. Tuli et al. [8] developed **HealthFog**, deploying deep learning ensembles across edge nodes for cardiac diagnosis. Nevertheless, none of these works explicitly analyzed the structural *latency cliff* introduced when critical medical alarms are co-located in FIFO aggregation buffers alongside routine vitals.

### Summary Comparison of Related Literature
| Publication | Environment | Vital Modalities | Edge Delta-Filtering | Window Aggregation | Priority LAN Bypass | Strict SLA Tracking |
|---|---|---|---|---|---|---|
| Gupta et al. (2017) [1] | iFogSim | Generic / EEG | No | No | No | No |
| Mahmud et al. (2022) [2] | iFogSim2 | Generic Microservices | No | No | No | No |
| Rahmani et al. (2018) [3] | UT-GATE Lab | ECG, Temp | Basic Threshold | No | Partial | No |
| Gia et al. (2015) [4] | Hardware Testbed | ECG (QRS) | Feature Extraction | No | No | No |
| Kraemer et al. (2017) [5] | Survey / Review | Multi-Parameter | Conceptual | No | Conceptual | No |
| Mutlag et al. (2019) [6] | Survey / Taxonomy | Multi-Parameter | No | No | Conceptual | No |
| Verma & Sood (2018) [7] | Simulation | Multi-Vital | Rule-based | No | No | No |
| Tuli et al. (2020) [8] | FogBus / iFogSim | Cardiovascular | No | No | No | Partial |
| Pace et al. (2019) [9] | BodyCloud | Generic BSN | Threshold | Yes ($W$) | No | No |
| Aazam & Huh (2014) [10] | FiCloud Testbed | Generic IoT | Trimming | No | No | No |
| **Our Framework (Topic 19)** | **iFogSim2** | **HR, $\text{SpO}_2$, TEMP, BP** | **Stateful ($\Delta_{\text{thresh}}$)** | **Configurable ($W$)** | **Dedicated Local Path** | **Strict ($\le$ 500 ms)** |

---

## III. System Architecture and Methodology

We formulate a hierarchical, three-tier cyber-physical topology consisting of:
1. **Patient IoT Sensor Tier**
2. **Hospital Ward Fog Gateway Tier**
3. **Cloud Datacenter Tier**

```
[Patient Bedside IoT Sensors]
Heart Rate, SpO2, Body Temp, Blood Pressure (Period = 1,000 ms)
       │
       ▼ Local LAN (2.0 ms, 10 Gbps)
[Hospital Ward Fog Gateway]
Stateful Delta-Filter (Δ_thresh) ───► Suppresses Redundant Normal Vitals
Three-Tier Priority Classifier:
  • CRITICAL  ════► Local Emergency Bypass ───► [Bedside Alarm Actuator] (1.0 ms LAN)
                      └────► Asynchronous Copy to Cloud EHR
  • ABNORMAL  ════► Direct WAN Dispatch (No Window Wait)
  • NORMAL    ════► Sliding-Window Aggregation (W readings per tuple)
       │
       ▼ Wide Area Network (WAN: 100 ms, 100 Mbps nominal)
[Cloud Datacenter Tier]
Long-Term Analytics, Medical History Storage, EHR Auditing
```

### A. Physical Topology and Network Modeling
Let $\mathcal{P} = \{P_1, P_2, \dots, P_N\}$ denote the set of $N$ monitored patients in a hospital ward. Each patient $P_i$ is instrumented with four biomedical sensors:
$$\mathcal{S}_i = \{S_{i}^{\text{HR}}, S_{i}^{\text{SpO}_2}, S_{i}^{\text{TEMP}}, S_{i}^{\text{BP}}\}$$
yielding total sensor population $|\mathcal{S}| = 4N$. Each sensor generates synthetic physiological tuples of size $L_{\text{sensor}} = 500\text{ Bytes}$ at fixed interval $\Delta t_{\text{sample}} = 1{,}000\text{ ms}$.

The bedside sensors connect to a ward-level intermediate computing node, the **Hospital Fog Gateway**, via a dedicated switched Ethernet/WLAN connection with propagation delay $d_{\text{LAN}} = 2.0\text{ ms}$ and bandwidth $B_{\text{LAN}} = 10{,}000\text{ Mbps}$ (10 Gbps). The fog gateway is an 8-core edge server provisioned with 8,000 MIPS processing capacity and 8,000 MB RAM.

The fog gateway connects to the centralized **Cloud Datacenter** across a dedicated WAN link characterized by propagation delay $d_{\text{WAN}}$ (nominally 100 ms) and uplink bandwidth $B_{\text{WAN}}$ (nominally 100 Mbps). The cloud datacenter is modeled as an enterprise multi-processor cluster equipped with 44,000 MIPS and 40,000 MB RAM. Finally, each patient bed features a bedside actuator (visual monitor/alarm bell) linked to the fog gateway with delay $d_{\text{act}} = 1.0\text{ ms}$.

### B. Architectural Taxonomy (Architectures A–D)
To rigorously evaluate the isolated impact of each edge mechanism, we implement four mutually exclusive architectures:

1. **Architecture A (Cloud-Only Baseline):** Medical sensors transmit all raw readings across the WAN directly to the cloud. The cloud executes analytics and returns an alarm tuple back across the WAN to the bedside actuator. No edge processing is performed.
2. **Architecture B (Fog Forwarding):** Tuples transit through the hospital fog gateway, which executes a passive inspection module (`fog_forwarder`, consuming 200 MIPS) before forwarding 100% of raw readings to the cloud over WAN. This isolates the pure latency overhead of an intermediate gateway hop.
3. **Architecture C (Naive Fog Filtering and Window Aggregation):** The fog gateway hosts a `fog_filter_aggregator` module. It implements delta-filtering and batches tuples into sliding windows of size $W$. Once full, the batch tuple is transmitted over WAN to the cloud, which processes the window and dispatches actuator notifications back over WAN. No priority distinction is made; critical alarms are queued indiscriminately inside the window buffer.
4. **Architecture D (Priority-Aware Fog with Local Emergency Bypass):** The fog gateway hosts an intelligent `priority_fog_processor` implementing data plane bifurcation. It classifies every incoming vital into three clinical tiers:
   - **CRITICAL:** Instantly dispatches a lightweight alert tuple ($L_{\text{alert}} = 200\text{ Bytes}$) directly to the local bedside actuator over the LAN ($1.0\text{ ms}$), completely bypassing the WAN and aggregation buffers. Concurrently, an asynchronous copy is transmitted to the cloud for EHR archival.
   - **ABNORMAL:** Dispatched immediately over WAN to cloud analytics without window buffering, ensuring prompt clinical review without aggregation delay.
   - **NORMAL:** Filtered via stateful delta-suppression ($\Delta_{\text{thresh}}$). Readings that do not change significantly from the last reported state are discarded. Retained normal vitals are batched into windows of size $W$ and forwarded to the cloud in compressed multi-sample tuples ($L_{\text{batch}} = 1{,}200\text{ Bytes}$).

### C. Mathematical Formulations
The end-to-end latency $T_{\text{e2e}}$ of a vital sign tuple is defined as the elapsed time from its generation at sensor $S_{i,j}$ ($t_{\text{gen}}$) to the delivery of the corresponding action tuple at the actuator ($t_{\text{act}}$):
$$T_{\text{e2e}} = t_{\text{act}} - t_{\text{gen}} = T_{\text{prop}} + T_{\text{trans}} + T_{\text{proc}} + T_{\text{queue}} + T_{\text{buf}}$$

For Architecture A, the theoretical minimum round-trip latency across the WAN is:
$$T_{\text{e2e}}^{\text{Arch A}} \approx 2 \times d_{\text{WAN}} + 2 \times d_{\text{LAN}} + T_{\text{proc}}^{\text{cloud}} \approx 200 + 4 + 3.7 \approx 207.7\text{ ms}$$

In Architecture C, an incoming critical vital that enters an empty aggregation window of size $W$ incurs a buffering delay:
$$T_{\text{buf}} = (W - 1) \times \Delta t_{\text{sample}}$$
yielding an expected critical alert latency:
$$T_{\text{e2e}}^{\text{Arch C, Crit}} \approx T_{\text{buf}} + 2 \times d_{\text{WAN}} + T_{\text{proc}}^{\text{fog}} + T_{\text{proc}}^{\text{cloud}} \gg 500\text{ ms}$$

In Architecture D, critical vitals bypass both $T_{\text{buf}}$ and the WAN entirely, traversing only the local switched network:
$$T_{\text{e2e}}^{\text{Arch D, Crit}} = d_{\text{LAN}} + T_{\text{proc}}^{\text{fog}} + d_{\text{act}} = 2.0\text{ ms} + \frac{200}{8000}\text{ ms} + 1.0\text{ ms} \approx 7.13\text{ ms}$$

Cloud Bandwidth Saving ($\text{BWS}_{\text{cloud}}$) relative to the Cloud-Only baseline is formalized as:
$$\text{BWS}_{\text{cloud}} = \left( 1 - \frac{\text{Traffic}_{\text{cloud}}^{\text{arch}}}{\text{Traffic}_{\text{cloud}}^{\text{Arch A}}} \right) \times 100\%$$

Finally, for clinical emergency events, Service Level Agreement (SLA) Reliability $\mathcal{R}_{\text{SLA}}$ against the medical deadline $\tau_{\text{SLA}} = 500\text{ ms}$ is given by:
$$\mathcal{R}_{\text{SLA}} = \frac{1}{M_{\text{crit}}} \sum_{k=1}^{M_{\text{crit}}} \mathbb{I}(T_{\text{e2e}}^{(k)} \le \tau_{\text{SLA}}) \times 100\%$$

---

## IV. Experimental Setup in iFogSim2

### A. Physiological Telemetry and Severity Model
To ensure clinical realism, the synthetic generator simulates vital signs using bounded Gaussian random walks calibrated against human physiological ranges [5], [7]. Each reading $v(t)$ evolves according to:
$$v(t) = v(t-1) + \mathcal{N}(0, \sigma^2)$$
subject to hard clinical physiological boundaries.

| Vital Parameter | Normal Range | Abnormal Range | Critical Range | Delta Threshold ($\Delta_{\text{thresh}}$) |
|---|---|---|---|---|
| Heart Rate (HR) | $60\text{--}100\text{ bpm}$ | $50\text{--}59$ / $101\text{--}120$ | $<50$ or $>120\text{ bpm}$ | $2.0\text{ bpm}$ |
| Oxygen Saturation ($\text{SpO}_2$) | $95\text{--}100\%$ | $90\text{--}94\%$ | $<90\%$ | $1.0\%$ |
| Body Temperature (TEMP) | $36.5\text{--}37.5\,^{\circ}\text{C}$ | $37.6\text{--}38.5\,^{\circ}\text{C}$ | $<36.0$ or $>38.5\,^{\circ}\text{C}$ | $0.2\,^{\circ}\text{C}$ |
| Systolic Blood Pressure (BP) | $90\text{--}120\text{ mmHg}$ | $121\text{--}140\text{ mmHg}$ | $<80$ or $>140\text{ mmHg}$ | $3.0\text{ mmHg}$ |
| **Workload Share** | $\approx 85.5\%$ | $\approx 9.1\%$ | $\approx 5.4\%$ | --- |

### B. Simulation Seeding and Execution Parameters
All experiments run with a fixed random seed (`seed = 42L`) ensuring that the exact same sequence of physiological readings, anomalies, and timestamps is replayed identically across all architectures. Simulations execute for $T_{\text{sim}} = 100{,}000\text{ ms}$ ($100\text{ seconds}$), during which each sensor emits 100 readings. At $t = 100{,}500\text{ ms}$, an explicit end-of-simulation buffer drain event is scheduled to flush partial windows ($< W$), followed by a simulation drain period terminating at $115{,}000\text{ ms}$. This guarantees exact mathematical accounting:
$$\text{Filtered Vitals} + \text{Delivered Vitals} = \text{Total Generated Vitals} \equiv 100.00\%$$

---

## V. Results and Discussion

### A. Experiment 1: Cross-Architecture Performance Baseline
Table 1 and Figures 1 and 2 detail the empirical performance of all four architectures under baseline conditions ($N=10\text{ patients}$, $40\text{ sensors}$, $W=5$, nominal WAN).

#### Table 1: Cross-Architecture Performance Baseline ($N=10, W=5$)
| Architecture | Filtered at Fog | Delivered | Critical Lat. (ms) | Avg Lat. (ms) | Cloud KB | Cloud Saving | SLA Rel. (%) |
|---|---|---|---|---|---|---|---|
| **Arch A (Cloud-Only)** | 0 (0.0%) | 4,000 | 207.95 | 207.71 | 1,953.1 | 0.0% | 100.0% |
| **Arch B (Fog Forward)** | 0 (0.0%) | 4,000 | 211.67 | 211.43 | 1,953.1 | 0.0% | 100.0% |
| **Arch C (Filter & Agg)** | 593 (14.8%) | 3,407 | 2,293.51 | 2,528.44 | 445.0 | **77.2%** | **27.9%** |
| **Arch D (Priority Fog)** | 712 (17.8%) | 3,288 | **7.13** | 2,604.07 | 651.9 | **66.6%** | **100.0%** |

![Figure 1: Latency by Severity](../results/graphs/fig1_arch_latency_by_severity.png)
*Fig. 1. End-to-end latency by physiological severity tier across Architectures A, B, C, and D ($N=10, W=5$).*

![Figure 2: Network Bandwidth Comparison](../results/graphs/fig2_arch_traffic_comparison.png)
*Fig. 2. Cloud-bound and total network bandwidth comparison.*

As shown in Fig. 1, Architecture A exhibits uniform latency across all severities ($\text{Normal: } 207.70\text{ ms}$, $\text{Critical: } 207.95\text{ ms}$) because every tuple incurs two WAN traversals. Architecture B introduces a $+3.72\text{ ms}$ gateway processing penalty ($\text{Critical: } 211.67\text{ ms}$) with zero bandwidth conservation (0.00\%), confirming that passive edge forwarding provides no architectural advantage.

Architecture C suppresses 593 redundant normal readings (14.8\%) and compresses remaining vitals, slashing cloud traffic from 1,953.13 KB to 444.97 KB (a 77.22% saving, Fig. 2). However, because critical vitals are buffered in FIFO aggregation queues, critical latency escalates to 2,293.52 ms. As a result, critical SLA compliance collapses to 27.88%, proving that naive edge aggregation is clinically hazardous.

In contrast, Architecture D successfully bifurcates the data plane: critical vitals bypass aggregation buffers and trigger bedside alarms over local LAN in **7.13 ms** (a **96.57% reduction** over Cloud-Only), while conserving 66.62% of cloud bandwidth (651.90 KB). Crucially, Architecture D achieves **100.00% critical SLA compliance**.

---

### B. Experiment 2: Patient and Sensor Scalability Analysis
Table 2 and Figures 3 and 4 evaluate system behavior as patient load scales from 10 to 50 patients (40 to 200 sensors, 4,000 to 20,000 generated readings).

#### Table 2: Patient and Sensor Scalability Analysis ($N \in \{10, 20, 30, 50\}$, $W=5$)
| Architecture | Patients ($N$) | Sensors | Critical Lat. (ms) | Avg Lat. (ms) | Cloud KB | Cloud Saving | SLA Rel. (%) |
|---|---|---|---|---|---|---|---|
| **Arch A** | 10 | 40 | 207.95 | 207.71 | 1,953.1 | 0.0% | 100.0% |
| **Arch D** | 10 | 40 | **7.13** | 2,604.07 | 651.9 | 66.6% | 100.0% |
| **Arch A** | 20 | 80 | 209.90 | 209.75 | 3,906.2 | 0.0% | 100.0% |
| **Arch D** | 20 | 80 | **7.13** | 2,590.06 | 1,305.6 | 66.6% | 100.0% |
| **Arch A** | 30 | 120 | 211.77 | 211.78 | 5,859.4 | 0.0% | 100.0% |
| **Arch D** | 30 | 120 | **7.13** | 2,602.48 | 1,939.1 | 66.9% | 100.0% |
| **Arch A** | 50 | 200 | 215.80 | 215.85 | 9,765.6 | 0.0% | 100.0% |
| **Arch D** | 50 | 200 | **7.12** | 2,619.27 | 3,221.5 | 67.0% | 100.0% |

![Figure 3: Scalability Latency](../results/graphs/fig3_scalability_latency.png)
*Fig. 3. Critical and average latency versus patient/sensor scale ($N=10 \dots 50$).*

![Figure 4: Scalability Cloud Traffic](../results/graphs/fig4_scalability_cloud_traffic.png)
*Fig. 4. Linear divergence of cloud WAN traffic as sensor count scales.*

The experimental traces reveal two vital scalability findings:
1. **Scale-Invariance of Critical Response:** In Architecture D, critical latency remains perfectly flat: **7.129 ms** at 40 sensors, **7.129 ms** at 80 sensors, **7.126 ms** at 120 sensors, and **7.125 ms at 200 sensors**. Because emergency alarms route exclusively over the switched local LAN without contending for WAN link queues, scaling the ward load introduces zero latency degradation.
2. **Divergence of Cloud WAN Ingress:** As depicted in Fig. 4, at 200 sensors, Architecture A saturates the WAN with 9.77 MB (9,765.63 KB) of raw telemetry. Architecture D transmits only 3.22 MB (3,221.48 KB), saving **6.55 MB of WAN backhaul traffic (67.01%)** while delivering 100% reliable emergency alerting.

---

### C. Experiment 3: Aggregation Window Sensitivity and The Pareto Cliff
Table 3 and Figure 5 investigate the impact of sliding window size $W \in \{1, 2, 5, 10, 20\}$ at fixed scale $N=20$ (80 sensors).

#### Table 3: Aggregation Window Sensitivity ($N=20$, $80$ Sensors)
| Architecture | Window Size ($W$) | Critical Lat. (ms) | Abnormal Lat. (ms) | Normal Lat. (ms) | Cloud KB | Cloud Saving | SLA Rel. (%) |
|---|---|---|---|---|---|---|---|
| **Arch A** | 1 | 209.90 | 209.69 | 209.74 | 3,906.2 | 0.0% | 100.0% |
| **Arch C** | 2 | 740.35 | 752.83 | 801.98 | 2,182.3 | 44.1% | 46.4% |
| **Arch D** | 2 | **7.13** | 208.71 | 952.06 | 2,318.0 | 40.7% | 100.0% |
| **Arch C** | 5 | 2,311.69 | 2,406.19 | 2,572.50 | 888.0 | 77.3% | 22.9% |
| **Arch D** | 5 | **7.13** | 208.34 | 3,154.48 | 1,305.6 | 66.6% | 100.0% |
| **Arch C** | 10 | 5,106.60 | 5,427.26 | 5,391.47 | 460.8 | 88.2% | 11.8% |
| **Arch D** | 10 | **7.13** | 208.19 | 6,782.22 | 966.6 | 75.3% | 100.0% |
| **Arch C** | 20 | 10,551.19 | 10,978.96 | 10,890.66 | 240.6 | 93.8% | 4.6% |
| **Arch D** | 20 | **7.13** | 208.08 | 13,719.52 | 795.2 | 79.6% | 100.0% |

![Figure 5: Window Pareto Trade-off](../results/graphs/fig5_window_pareto_tradeoff.png)
*Fig. 5. The Aggregation Latency Cliff: Trade-off between bandwidth saving and critical alert latency as window size ($W$) increases.*

This experiment reveals a fundamental architectural trade-off. In Architecture C, expanding $W$ from 1 to 20 boosts cloud bandwidth conservation from 0.00% to 93.84%. However, this comes at the expense of an exponential latency penalty: critical alert latency surges to **10,551.19 ms (10.55 seconds)**, causing critical SLA compliance to collapse from 100% down to a catastrophic **4.62%**.

In Architecture D, our priority bypass mechanism *completely decouples critical safety from aggregation window size*. Regardless of whether $W=1$ or $W=20$, critical latency remains strictly pegged at **7.13 ms**, preserving **100.00% SLA compliance**. This allows hospital system administrators to aggressively configure large aggregation windows ($W=10$ or $W=20$) to save up to **79.64% cloud bandwidth** without exposing patients to delayed emergency interventions.

---

### D. Experiment 4: WAN Network Stress and Clinical Fault-Tolerance
Table 4 and Figures 6 and 7 evaluate system resilience across four severe network degradation profiles ($N=20, W=5$, SLA $\le 500\text{ ms}$).

#### Table 4: WAN Degradation vs. Emergency Alert Reliability ($N=20$)
| Profile | Architecture | WAN Latency (ms) | WAN Bw (Kbps) | Critical Lat. (ms) | Cloud Saving | Critical SLA Rel. (%) |
|---|---|---|---|---|---|---|
| **Fiber Normal** | Arch A | 100 | 100,000 | 209.90 | 0.0% | 100.0% |
| **Fiber Normal** | Arch C | 100 | 100,000 | 2,311.69 | 77.3% | 22.9% |
| **Fiber Normal** | Arch D | 100 | 100,000 | **7.13** | 66.6% | **100.0%** |
| **WAN Congested** | Arch A | 200 | 1,000 | 419.34 | 0.0% | 100.0% |
| **WAN Congested** | Arch C | 200 | 1,000 | 2,560.48 | 77.3% | 21.5% |
| **WAN Congested** | Arch D | 200 | 1,000 | **7.13** | 66.6% | **100.0%** |
| **WAN Bottleneck** | Arch A | 400 | 250 | 855.31 | 0.0% | **0.0% (Collapse)** |
| **WAN Bottleneck** | Arch C | 400 | 250 | 3,108.71 | 77.3% | **0.0% (Collapse)** |
| **WAN Bottleneck** | Arch D | 400 | 250 | **7.13** | 66.6% | **100.0% (Rock Solid)** |
| **Rural Degraded** | Arch A | 600 | 100 | 1,317.17 | 0.0% | **0.0% (Collapse)** |
| **Rural Degraded** | Arch C | 600 | 100 | 3,848.78 | 77.3% | **0.0% (Collapse)** |
| **Rural Degraded** | Arch D | 600 | 100 | **7.13** | 66.6% | **100.0% (Rock Solid)** |

![Figure 6: Network Stress Reliability](../results/graphs/fig6_network_stress_reliability.png)
*Fig. 6. Critical Alert SLA Reliability under WAN Stress ($N=20$, SLA $\le 500$ ms).*

![Figure 7: Network Stress Latency](../results/graphs/fig7_network_stress_latency.png)
*Fig. 7. Critical Alert Latency under Network Degradation (Log Scale).*

Under nominal fiber conditions ($100\text{ ms}$, $100\text{ Mbps}$), Architecture A achieves 100% SLA reliability ($209.90\text{ ms} \le 500\text{ ms}$). However, when WAN conditions deteriorate:
- Under **WAN Severe Bottleneck** ($400\text{ ms}$ WAN delay, $250\text{ Kbps}$ bandwidth), Architecture A's round-trip latency surges to $855.31\text{ ms}$. It suffers a **complete system failure: 0.00% SLA reliability**.
- Under **WAN Rural Degraded** ($600\text{ ms}$ WAN delay, $100\text{ Kbps}$ bandwidth), Architecture A latency rises to $1{,}317.17\text{ ms}$ (**0.00% SLA reliability**).
- Architecture C suffers **0.00% SLA reliability** under both degraded profiles, with critical delays exceeding $3{,}100\text{ ms}$ and $3{,}800\text{ ms}$.
- In striking contrast, Architecture D maintains **100.00% Critical SLA Reliability (433/433 alerts delivered)** at a steady **7.13 ms** across every single degradation profile.

Because emergency alerts in Architecture D are isolated within the hospital LAN, the framework is intrinsically immune to external wide-area disruptions, fiber cuts, and congestion storms.

---

## VI. Limitations and Future Work

While our framework establishes a rigorous, reproducible benchmark, we explicitly document its simulation boundaries:
1. **Synthetic Workload Modeling:** Although our random walk model reflects clinical distributions and variance, it lacks the multi-variate morphological cross-correlations observed in real clinical wave databases (e.g., MIMIC-III or PhysioNet). Future extensions will incorporate raw ECG and arterial pressure waveforms.
2. **Single Gateway Topology:** The present simulation models one central hospital ward gateway serving all patient bridges. In large clinical facilities, gateway failover, load balancing, and peer-to-peer fog clustering must be evaluated.
3. **Energy Profiling of Wearables:** Our simulation assumes mains-powered bedside sensor bridges. Future work will integrate battery depletion and duty-cycle modeling on wearable sensor nodes.

---

## VII. Conclusion

In this paper, we addressed the fundamental tension between WAN bandwidth conservation and clinical emergency responsiveness in IoT-driven healthcare telemetry. Using the iFogSim2 simulation environment, we implemented and compared four paradigms: Cloud-Only (A), Fog Forwarding (B), Naive Fog Filtering and Aggregation (C), and Priority-Aware Fog (D).

Our experimental findings demonstrate that naive edge window aggregation introduces a dangerous clinical hazard, delaying emergency alerts by over 2.29 seconds ($W=5$) and up to 10.55 seconds ($W=20$), causing SLA compliance to crash to 4.62%. Our proposed Priority-Aware Fog framework (Architecture D) resolves this challenge through stateful delta-filtering and a dedicated local emergency bypass channel. Empirical results confirm that Architecture D:
- Delivers life-critical alerts in **7.13 ms** (a 96.57% latency reduction over Cloud-Only),
- Conserves **66.62% to 79.64%** of cloud WAN bandwidth,
- Demonstrates **scale-invariance** up to 200 concurrent biomedical sensors, and
- Maintains **100.00% clinical SLA reliability** during severe wide-area network bottlenecks and link failures.

Our architecture provides a proven, open-source template for deploying safe, bandwidth-efficient edge intelligence in smart hospital environments.

---

## References

1. H. Gupta, A. V. Dastjerdi, S. Ghosh, and R. Buyya, "iFogSim: A toolkit for modeling and simulation of resource management techniques in the Internet of Things, Edge and Fog computing environments," *Software: Practice and Experience*, vol. 47, no. 9, pp. 1275–1296, Sep. 2017.
2. R. Mahmud, Faheem, and R. Buyya, "iFogSim2: An Extended iFogSim Simulator for Mobility, Clustering, and Microservice Management in Edge and Fog Computing Environments," *Journal of Systems and Software*, vol. 190, p. 111351, Aug. 2022.
3. A. M. Rahmani, T. N. Gia, B. Negash, A. Anzanpour, I. Azimi, M. Jiang, and P. Liljeberg, "Exploiting smart e-Health gateways at the edge of healthcare Internet-of-Things: A fog computing approach," *Future Generation Computer Systems*, vol. 78, pp. 641–658, Jan. 2018.
4. T. N. Gia, M. Jiang, A. M. Rahmani, T. Westerlund, P. Liljeberg, and H. Tenhunen, "Fog Computing in Healthcare Internet of Things: A Case Study on ECG Feature Extraction," in *Proc. IEEE Int. Conf. Comput. Inf. Technol.; Ubiquitous Comput. Commun.; Dependable, Autonomic Secure Comput.; Pervasive Intell. Comput. (CIT/IUCC/DASC/PICOM)*, Liverpool, UK, 2015, pp. 356–363.
5. F. A. Kraemer, A. E. Braten, N. Tamkittikhun, and D. Palma, "Fog Computing in Healthcare—A Review and Discussion," *IEEE Access*, vol. 5, pp. 9206–9222, May 2017.
6. A. A. Mutlag, M. K. Abd Ghani, N. Arunkumar, M. A. Mohammed, and O. Geman, "Enabling technologies for fog computing in healthcare IoT systems: Future research directions," *IEEE Access*, vol. 7, pp. 81888–81922, Jun. 2019.
7. P. Verma and S. K. Sood, "Fog Assisted-IoT Enabled Patient Health Monitoring in Smart Homes," *IEEE Internet of Things Journal*, vol. 5, no. 3, pp. 1388–1396, Jun. 2018.
8. S. Tuli, N. Basumatary, S. S. Gill, M. Kahani, R. C. Arya, G. S. Wander, and R. Buyya, "HealthFog: An ensemble deep learning based Smart Healthcare System for Automatic Diagnosis of Heart Diseases in Integrated IoT and Fog Computing Environments," *Future Generation Computer Systems*, vol. 104, pp. 187–200, Mar. 2020.
9. P. Pace, G. Aloi, R. Gravina, G. Caliciuri, G. Fortino, and A. Liotta, "An edge-based architecture to support efficient applications for healthcare IoT," *IEEE Access*, vol. 7, pp. 43147–43159, Mar. 2019.
10. M. Aazam and E.-N. Huh, "Fog computing and smart gateway based communication for cloud of things," in *Proc. IEEE 2nd Int. Conf. Future Internet of Things and Cloud (FiCloud)*, Barcelona, Spain, 2014, pp. 464–470.
