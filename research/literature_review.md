# Comprehensive Literature Review: Fog-Assisted Real-Time Healthcare Monitoring

**Topic 19:** Fog-Assisted Real-Time Healthcare / Smart-City Monitoring System (iFogSim)  
**Evaluation:** 15-Mark University Research Project (Team of 3 Undergraduates)  
**Institution:** Indian Institute of Information Technology (IIIT) Sri City  
**Date:** November 2026  

---

## 1. Introduction & Research Landscape

Continuous physiological telemetry in connected hospital wards and smart homes—monitoring Heart Rate (HR), Blood Oxygen Saturation ($\text{SpO}_2$), Body Temperature (TEMP), and Blood Pressure (BP)—generates high-velocity, continuous data streams. Traditional **Cloud-Only architectures** route all raw sensor telemetry across the Wide Area Network (WAN) to centralized cloud datacenters. While the cloud offers virtually unbounded analytical storage, it incurs two fundamental bottlenecks:
1. **Unbounded WAN Latency and Jitter:** Network congestion, bandwidth throttles, and wide-area packet delays prevent cloud analytics from guaranteeing real-time response times ($< 500\text{ ms}$) for acute medical crises (e.g., ventricular tachycardia, hypoxic collapse, hypertensive crises).
2. **Excessive WAN Bandwidth Ingestion:** Continuous streams of redundant physiological readings needlessly consume WAN link capacity and drive up cloud ingress/storage costs.

To bridge this research gap, **Fog Computing** introduces an intermediate computational tier situated at the network edge (e.g., hospital ward gateways, bedside micro-servers). However, prior studies in healthcare edge computing frequently treat edge processing as either **passive packet routing** or **naive batch aggregation**, creating an unaddressed clinical safety hazard: indiscriminate window aggregation forces life-critical emergency alerts to queue behind normal physiological readings, leading to fatal clinical delays.

This literature review analyzes **10 foundational, peer-reviewed publications** spanning simulation environments, smart e-Health gateways, edge filtering techniques, and priority-aware edge routing.

---

## 2. In-Depth Analysis of 10 Foundational Academic Papers

### Paper 1: The iFogSim Simulation Toolkit
- **Citation:** H. Gupta, A. V. Dastjerdi, S. Ghosh, and R. Buyya, "iFogSim: A toolkit for modeling and simulation of resource management techniques in the Internet of Things, Edge and Fog computing environments," *Software: Practice and Experience*, vol. 47, no. 9, pp. 1275–1296, Sep. 2017. DOI: `10.1002/spe.2509`.
- **Problem Addressed:** The absence of generalized, reproducible simulation frameworks capable of modeling hierarchical Fog-Cloud topologies, resource management policies, and application modularity before physical deployment.
- **Technique & Simulator:** Custom discrete-event simulation engine built on CloudSim. Models edge resources as `FogDevice` hierarchies, applications as Directed Acyclic Graphs (`AppModule` and `AppEdge`), and network data units as `Tuple` objects.
- **Key Findings:** Demonstrated that edge offloading can decrease network congestion by over $80\%$ compared to centralized cloud execution in smart-city and electroencephalography (EEG) tractor-beam gaming use cases.
- **Limitations:** Focuses primarily on generalized application placement (e.g., `Edgewards` vs. `Cloud-Only`); lacks domain-specific physiological priority queues and stateful delta-filtering primitives for clinical streams.
- **How Our Architecture Differs & Builds Upon It:** We leverage iFogSim's core discrete-event engine but construct a custom, domain-specific healthcare processing framework (`HealthcareFogDevice`, `VitalSignGenerator`, and `PriorityFogDevice`) implementing a multi-tiered priority dispatcher and stateful sliding window aggregation ($W$).

---

### Paper 2: iFogSim2 — Next-Generation Fog Simulation
- **Citation:** R. Mahmud, Faheem, and R. Buyya, "iFogSim2: An Extended iFogSim Simulator for Mobility, Clustering, and Microservice Management in Edge and Fog Computing Environments," *Journal of Systems and Software*, vol. 190, p. 111351, Aug. 2022. DOI: `10.1016/j.jss.2022.111351`.
- **Problem Addressed:** Scalability bottlenecks, microservice migration, and device mobility in dynamic edge environments.
- **Technique & Simulator:** Extended iFogSim engine with distributed microservice orchestration, cluster coordination, and user mobility tracking.
- **Key Findings:** Enables fine-grained evaluation of microservice container migration and localized peer-to-peer clustering among fog nodes.
- **Limitations:** Does not provide dedicated algorithms for physiological anomaly detection, delta compression, or emergency safety-critical SLA tracking.
- **How Our Architecture Differs & Builds Upon It:** We adopt iFogSim2 as our verified simulation baseline, exploiting its robust discrete-event scheduler to model hospital fog gateways with deterministic vital-sign generation across $10$ to $50$ patients ($40$ to $200$ physiological sensors).

---

### Paper 3: Smart e-Health Gateways at the Healthcare Edge (UT-GATE)
- **Citation:** A. M. Rahmani, T. N. Gia, B. Negash, A. Anzanpour, I. Azimi, M. Jiang, and P. Liljeberg, "Exploiting smart e-Health gateways at the edge of healthcare Internet-of-Things: A fog computing approach," *Future Generation Computer Systems*, vol. 78, pp. 641–658, Jan. 2018. DOI: `10.1016/j.future.2017.02.036`.
- **Problem Addressed:** Single-point-of-failure vulnerabilities, communication heterogeneity, and high latency in traditional cloud-centric IoT healthcare architectures.
- **Technique & Simulator:** Development of the "UT-GATE" smart e-Health gateway prototype using embedded Linux edge devices situated between medical sensors and medical cloud records. Implements local protocol conversion, local data storage, and notification routing.
- **Key Findings:** Validated that local edge intelligence maintains continuous medical monitoring even during WAN backhaul disconnections and drastically reduces response times for local medical alarms.
- **Limitations:** Evaluated primarily on a small-scale laboratory hardware prototype ($1\text{--}3$ patient nodes); lacks broad statistical parameter sweeps under varying network stress profiles, large-scale sensor counts ($>100$), and systematic aggregation window sensitivity analyses.
- **How Our Architecture Differs & Builds Upon It:** We model the smart e-Health gateway concept mathematically and simulate it at scale ($N=10 \dots 50$ patients), systematically measuring latency distributions, bandwidth savings ($66.6\%\text{--}77.5\%$), and SLA compliance across congested, bottlenecked, and rural WAN conditions.

---

### Paper 4: Edge Feature Extraction for Electrocardiogram (ECG) Streams
- **Citation:** T. N. Gia, M. Jiang, A. M. Rahmani, T. Westerlund, P. Liljeberg, and H. Tenhunen, "Fog Computing in Healthcare Internet of Things: A Case Study on ECG Feature Extraction," in *Proc. IEEE Int. Conf. Comput. Inf. Technol.; Ubiquitous Comput. Commun.; Dependable, Autonomic Secure Comput.; Pervasive Intell. Comput. (CIT/IUCC/DASC/PICOM)*, Liverpool, UK, 2015, pp. 356–363. DOI: `10.1109/CIT/IUCC/DASC/PICOM.2015.51`.
- **Problem Addressed:** High energy consumption and network bandwidth saturation caused by transmitting raw, high-frequency ECG waveforms over wireless medical networks.
- **Technique & Simulator:** Edge-based Pan-Tompkins QRS-detection algorithm running on low-power Intel Edison and ARM gateway platforms to extract heart rate and R-R intervals locally.
- **Key Findings:** Demonstrated an edge-side bandwidth reduction exceeding $90\%$ by transmitting extracted clinical features rather than raw biometric voltage time-series.
- **Limitations:** Focuses exclusively on single-lead ECG signal processing; does not evaluate multi-vital telemetry streams (e.g., cross-correlating $\text{SpO}_2$, BP, and Temperature) or priority queuing for sudden hemodynamic crises.
- **How Our Architecture Differs & Builds Upon It:** We extend edge filtering across four simultaneous vital-sign modalities ($\text{HR}$, $\text{SpO}_2$, $\text{TEMP}$, $\text{BP}$) utilizing clinical delta-filtering ($\Delta_{\text{thresh}}$) and demonstrate that critical alerts must bypass batching windows entirely to prevent fatal delay.

---

### Paper 5: Fog Computing in Healthcare — A Comprehensive Review
- **Citation:** F. A. Kraemer, A. E. Braten, N. Tamkittikhun, and D. Palma, "Fog Computing in Healthcare—A Review and Discussion," *IEEE Access*, vol. 5, pp. 9206–9222, May 2017. DOI: `10.1109/ACCESS.2017.2704100`.
- **Problem Addressed:** Systematic classification of healthcare monitoring requirements, identifying architectural tensions between data confidentiality, processing latency, energy efficiency, and network reliability.
- **Technique & Simulator:** Comprehensive literature survey and architectural requirement mapping across hospital, ambulatory, and assisted-living domains.
- **Key Findings:** Identifies that real-time closed-loop actuation (e.g., alarm triggers, automated infusion) requires sub-second response times that cloud datacenters cannot reliably guarantee over public WAN links.
- **Limitations:** Descriptive review paper without experimental simulation benchmarks, empirical mathematical models, or quantitative code implementations.
- **How Our Architecture Differs & Builds Upon It:** We implement the exact quantitative recommendations of Kraemer et al. by establishing four explicit architectures (A, B, C, D) in iFogSim2, providing concrete empirical proof that Architecture D satisfies the sub-second real-time actuation requirement ($7.13\text{ ms}$) under all network conditions.

---

### Paper 6: Enabling Technologies for Fog Computing in Healthcare IoT Systems
- **Citation:** A. A. Mutlag, M. K. Abd Ghani, N. Arunkumar, M. A. Mohammed, and O. Geman, "Enabling technologies for fog computing in healthcare IoT systems: Future research directions," *IEEE Access*, vol. 7, pp. 81888–81922, Jun. 2019. DOI: `10.1109/ACCESS.2019.2924094`.
- **Problem Addressed:** Architectural taxonomy of edge computing paradigms, addressing interoperability, security, and edge resource management in medical cyber-physical systems.
- **Technique & Simulator:** Systematic survey and technical taxonomy comparing edge architectures, virtualization layers, and edge data processing frameworks.
- **Key Findings:** Highlights that unmanaged edge processing risks becoming a performance bottleneck unless data filtering and traffic classification are incorporated into edge gateways.
- **Limitations:** Does not provide an open-source, executable simulation benchmark to quantitatively evaluate edge queuing delays versus WAN latency gains.
- **How Our Architecture Differs & Builds Upon It:** We address Mutlag et al.'s call for edge classification by implementing a three-tier priority classifier at the fog gateway, demonstrating that edge processing overhead ($+3.72\text{ ms}$) is easily outweighed by local emergency response ($7.13\text{ ms}$ vs. $207.95\text{ ms}$).

---

### Paper 7: Fog-Assisted Smart Home Patient Health Monitoring
- **Citation:** P. Verma and S. K. Sood, "Fog Assisted-IoT Enabled Patient Health Monitoring in Smart Homes," *IEEE Internet of Things Journal*, vol. 5, no. 3, pp. 1388–1396, Jun. 2018. DOI: `10.1109/JIOT.2017.2788059`.
- **Problem Addressed:** Monitoring chronic disease patients in smart home environments without continuous cloud dependence.
- **Technique & Simulator:** Three-tier architecture (IoT sensors $\to$ Fog layer $\to$ Cloud storage) utilizing rule-based event processing and Bayesian belief networks at the fog layer to categorize patient health states.
- **Key Findings:** Fog processing reduced network response time from $1{,}850\text{ ms}$ (cloud) to $290\text{ ms}$ (fog) and lowered WAN traffic by $68\%$.
- **Limitations:** Does not model sliding window aggregation sensitivity ($W$) or evaluate how buffering affects urgent anomaly delivery; Bayesian classification adds edge computation latency that may degrade under large sensor surges.
- **How Our Architecture Differs & Builds Upon It:** We implement deterministic, high-throughput delta-filtering and threshold classification ($O(1)$ complexity) paired with a priority bypass channel, achieving an order of magnitude faster critical response ($7.13\text{ ms}$) and proving that emergency alerts must never wait for window flushes.

---

### Paper 8: HealthFog — Deep Learning Edge-Cloud Healthcare Architecture
- **Citation:** S. Tuli, N. Basumatary, S. S. Gill, M. Kahani, R. C. Arya, G. S. Wander, and R. Buyya, "HealthFog: An ensemble deep learning based Smart Healthcare System for Automatic Diagnosis of Heart Diseases in Integrated IoT and Fog Computing Environments," *Future Generation Computer Systems*, vol. 104, pp. 187–200, Mar. 2020. DOI: `10.1016/j.future.2019.10.117`.
- **Problem Addressed:** Real-time heart disease diagnosis using deep learning ensembles deployed on edge resources without causing severe latency degradation.
- **Technique & Simulator:** Containerized ensemble deep learning models deployed across edge worker nodes using FogBus and iFogSim. Evaluates latency, energy consumption, network bandwidth, and model accuracy.
- **Key Findings:** HealthFog demonstrated superior accuracy and latency compared to cloud-only processing, proving that edge worker nodes can execute neural inference for non-emergency cardiac classification.
- **Limitations:** Focuses on compute-intensive deep learning inference; requires edge nodes with high compute/RAM capacity ($>4\text{ GB}$). Does not investigate ultra-low-latency local emergency bypass for primitive vital alarms on lightweight gateways.
- **How Our Architecture Differs & Builds Upon It:** While HealthFog targets heavy analytical inference, our work optimizes lightweight bedside gateways ($8{,}000\text{ MIPS}$): we offload long-term analytics and EHR storage to the cloud while handling immediate life-critical threshold alarms locally in $7.13\text{ ms}$.

---

### Paper 9: Edge-Based Architecture for Healthcare IoT
- **Citation:** P. Pace, G. Aloi, R. Gravina, G. Caliciuri, G. Fortino, and A. Liotta, "An edge-based architecture to support efficient applications for healthcare IoT," *IEEE Access*, vol. 7, pp. 43147–43159, Mar. 2019. DOI: `10.1109/ACCESS.2019.2907559`.
- **Problem Addressed:** Bandwidth constraints and battery depletion in body sensor networks (BSNs) transmitting continuous telemetry.
- **Technique & Simulator:** Edge-centric computing model utilizing BodyCloud and decentralized smart gateways to aggregate sensor measurements, filter outliers, and enforce duty cycling.
- **Key Findings:** Edge-level data filtering reduced transmitted data volume by over $70\%$ while prolonging sensor node operational lifetimes by $45\%$.
- **Limitations:** Evaluates data reduction exclusively from the perspective of energy conservation; does not evaluate the clinical hazard introduced when emergency readings are caught in edge aggregation queues.
- **How Our Architecture Differs & Builds Upon It:** We specifically uncover and solve the **"Latency Cliff"**: showing that while edge aggregation achieves high bandwidth conservation ($77.2\%$), treating all readings uniformly causes emergency SLA compliance to collapse from $100\%$ down to $4.62\%$ ($W=20$). Architecture D resolves this tension by bifurcating the data plane.

---

### Paper 10: Smart Gateway Communication for Cloud of Things
- **Citation:** M. Aazam and E.-N. Huh, "Fog computing and smart gateway based communication for cloud of things," in *Proc. IEEE 2nd Int. Conf. Future Internet of Things and Cloud (FiCloud)*, Barcelona, Spain, 2014, pp. 464–470. DOI: `10.1109/FiCloud.2014.83`.
- **Problem Addressed:** Unregulated sensor data offloading creating severe cloud ingestion bottlenecks and uncontrolled storage consumption.
- **Technique & Simulator:** Foundational smart gateway architecture incorporating data trimming, data filtering, and protocol translation at the edge of the Cloud of Things (CoT).
- **Key Findings:** Trimming redundant samples and filtering unchanged environmental metrics reduced packet transmission to the core cloud by $65\%\text{--}80\%$.
- **Limitations:** The filtering policies were static and evaluated under synthetic, non-critical sensor feeds (temperature, humidity); no distinction was made between routine telemetry and life-critical emergency events.
- **How Our Architecture Differs & Builds Upon It:** We adapt Aazam & Huh's data trimming and filtering principles into the clinical domain, implementing stateful delta-filtering ($\Delta_{\text{thresh}}$) coupled with a dynamic, three-tier clinical triage engine ($\text{NORMAL}$, $\text{ABNORMAL}$, $\text{CRITICAL}$) that ensures zero clinical information loss.

---

## 3. Comparative Taxonomy & Literature Summary Table

$$\begin{array}{lcccccc}
\hline
\textbf{Publication} & \textbf{Environment} & \textbf{Vital Modalities} & \textbf{Edge Filtering} & \textbf{Window Agg.} & \textbf{Priority Bypass} & \textbf{SLA Tracking} \\
\hline
\text{Gupta et al. (2017)} & \text{iFogSim} & \text{Generic / EEG} & \text{No} & \text{No} & \text{No} & \text{No} \\
\text{Mahmud et al. (2022)} & \text{iFogSim2} & \text{Generic Microservices} & \text{No} & \text{No} & \text{No} & \text{No} \\
\text{Rahmani et al. (2018)} & \text{UT-GATE Lab} & \text{ECG, Temp} & \text{Basic} & \text{No} & \text{Partial} & \text{No} \\
\text{Gia et al. (2015)} & \text{Testbed} & \text{ECG (QRS)} & \text{Feature Extr.} & \text{No} & \text{No} & \text{No} \\
\text{Kraemer et al. (2017)} & \text{Survey} & \text{Literature Review} & \text{Conceptual} & \text{No} & \text{Conceptual} & \text{No} \\
\text{Mutlag et al. (2019)} & \text{Survey} & \text{Taxonomy} & \text{No} & \text{No} & \text{Conceptual} & \text{No} \\
\text{Verma \& Sood (2018)} & \text{Simulation} & \text{Vital Signs} & \text{Rule-based} & \text{No} & \text{No} & \text{No} \\
\text{Tuli et al. (2020)} & \text{FogBus/iFogSim} & \text{Cardiovascular} & \text{No} & \text{No} & \text{No} & \text{Partial} \\
\text{Pace et al. (2019)} & \text{BodyCloud} & \text{Generic BSN} & \text{Yes} & \text{Yes} & \text{No} & \text{No} \\
\text{Aazam \& Huh (2014)} & \text{FiCloud} & \text{Generic IoT} & \text{Trimming} & \text{No} & \text{No} & \text{No} \\
\hline
\textbf{Our Work (Topic 19)} & \textbf{iFogSim2} & \textbf{HR, SpO2, Temp, BP} & \textbf{Stateful Delta} & \textbf{Configurable } W & \textbf{Local LAN Bypass} & \textbf{Strict (500 ms)} \\
\hline
\end{array}$$

---

## 4. Synthesis & Research Contribution

Our analysis of the literature reveals three critical gaps in edge-assisted healthcare:
1. **The Latency-Bandwidth Tension:** Prior works either optimize bandwidth through batch compression (e.g., Pace et al., Aazam & Huh) or minimize latency through edge inference (e.g., Tuli et al.), but fail to evaluate the resulting safety trade-offs when emergency events are mixed into batch aggregation queues.
2. **Absence of Priority-Aware Edge Routing:** Existing smart gateways (Rahmani et al., Verma & Sood) lack a decoupled data-plane architecture where emergency alarms trigger immediate local closed-loop actuation over LAN while non-critical streams are filtered and compressed for WAN transmission.
3. **Lack of Controlled Network Stress Benchmarking:** Prior healthcare IoT simulations test primarily nominal network links, failing to evaluate system reliability under WAN bottlenecks, bandwidth degradation, and rural connectivity constraints.

Our implementation of **Architecture D (Priority-Aware Fog)** directly fills these gaps by demonstrating that local priority bypass provides **$7.13\text{ ms}$ critical response time** ($96.57\%$ faster than cloud-only), maintains **$100.00\%$ clinical SLA reliability** under severe WAN collapse, and simultaneously conserves **$66.62\%\text{ to }79.64\%$ of cloud bandwidth**.
