# Empirical Summary Tables: Topic 19 Healthcare Fog Simulation

This document contains publication-ready summary tables derived dynamically from our Phase 4 experimental datasets.

---

## Table 1: Cross-Architecture Comparison ($N=10$ Patients, $40$ Sensors, $W=5$)
*(Source: `results/csv/exp1_architecture_comparison.csv`)*

| Architecture | Filtered at Fog | Total Delivered | Critical Delivery (%) | Critical Latency (ms) | Avg Latency (ms) | Cloud Traffic (KB) | Cloud BW Saving (%) | Critical SLA Rel. (%) |
|---|---|---|---|---|---|---|---|---|
| Arch A (Cloud-Only) | 0 (0.0%) | 4,000 | 100.00% | **207.954** | 207.711 | 1953.13 | **0.00%** | **100.00%** |
| Arch B (Fog Forward) | 0 (0.0%) | 4,000 | 100.00% | **211.673** | 211.432 | 1953.13 | **0.00%** | **100.00%** |
| Arch C (Filter & Agg) | 593 (14.8%) | 3,407 | 100.00% | **2293.515** | 2528.443 | 444.97 | **77.22%** | **27.88%** |
| Arch D (Priority Fog) | 712 (17.8%) | 3,288 | 100.00% | **7.129** | 2604.071 | 651.90 | **66.62%** | **100.00%** |

---

## Table 2: Patient & Sensor Scalability ($N \in \{10, 20, 30, 50\}$)
*(Source: `results/csv/exp2_scalability.csv`)*

| Architecture | Patients ($N$) | Sensors | Total Generated | Critical Latency (ms) | Normal Latency (ms) | Avg Latency (ms) | Cloud Traffic (KB) | Cloud BW Saving (%) | Critical SLA Rel. (%) |
|---|---|---|---|---|---|---|---|---|---|
| Arch A | 10 | 40 | 4,000 | 207.954 | 207.704 | 207.711 | 1953.13 | 0.00% | 100.00% |
| Arch B | 10 | 40 | 4,000 | 211.673 | 211.424 | 211.432 | 1953.13 | 0.00% | 100.00% |
| Arch C | 10 | 40 | 4,000 | 2293.515 | 2565.268 | 2528.443 | 444.97 | 77.22% | 27.88% |
| Arch D | 10 | 40 | 4,000 | 7.129 | 3182.284 | 2604.071 | 651.90 | 66.62% | 100.00% |
| Arch A | 20 | 80 | 8,000 | 209.899 | 209.744 | 209.747 | 3906.25 | 0.00% | 100.00% |
| Arch B | 20 | 80 | 8,000 | 213.236 | 213.050 | 213.056 | 3906.25 | 0.00% | 100.00% |
| Arch C | 20 | 80 | 8,000 | 2311.691 | 2572.502 | 2536.424 | 888.04 | 77.27% | 22.86% |
| Arch D | 20 | 80 | 8,000 | 7.129 | 3154.482 | 2590.063 | 1305.57 | 66.58% | 100.00% |
| Arch A | 30 | 120 | 12,000 | 211.769 | 211.785 | 211.783 | 5859.38 | 0.00% | 100.00% |
| Arch B | 30 | 120 | 12,000 | 214.682 | 214.684 | 214.680 | 5859.38 | 0.00% | 100.00% |
| Arch C | 30 | 120 | 12,000 | 2378.269 | 2576.534 | 2556.235 | 1319.04 | 77.49% | 20.58% |
| Arch D | 30 | 120 | 12,000 | 7.126 | 3153.929 | 2602.477 | 1939.06 | 66.91% | 100.00% |
| Arch A | 50 | 200 | 20,000 | 215.796 | 215.854 | 215.854 | 9765.63 | 0.00% | 100.00% |
| Arch B | 50 | 200 | 20,000 | 217.905 | 217.932 | 217.930 | 9765.63 | 0.00% | 100.00% |
| Arch C | 50 | 200 | 20,000 | 2419.802 | 2597.017 | 2567.771 | 2195.65 | 77.52% | 19.90% |
| Arch D | 50 | 200 | 20,000 | 7.125 | 3179.205 | 2619.266 | 3221.48 | 67.01% | 100.00% |

---

## Table 3: Aggregation Window ($W$) Sensitivity ($N=20$, $80$ Sensors)
*(Source: `results/csv/exp3_aggregation_window.csv`)*

| Architecture | Window Size ($W$) | Critical Latency (ms) | Abnormal Latency (ms) | Normal Latency (ms) | Avg Latency (ms) | Cloud Traffic (KB) | Cloud BW Saving (%) | Critical SLA Rel. (%) |
|---|---|---|---|---|---|---|---|---|
| Arch A | 1 | **209.899** | 209.692 | 209.744 | 209.747 | 3906.25 | **0.00%** | **100.00%** |
| Arch C | 2 | **740.349** | 752.832 | 801.980 | 792.297 | 2182.32 | **44.13%** | **46.42%** |
| Arch D | 2 | **7.129** | 208.714 | 952.062 | 799.766 | 2318.02 | **40.66%** | **100.00%** |
| Arch C | 5 | **2311.691** | 2406.190 | 2572.502 | 2536.424 | 888.04 | **77.27%** | **22.86%** |
| Arch D | 5 | **7.129** | 208.344 | 3154.482 | 2590.063 | 1305.57 | **66.58%** | **100.00%** |
| Arch C | 10 | **5106.601** | 5427.256 | 5391.465 | 5377.618 | 460.84 | **88.20%** | **11.78%** |
| Arch D | 10 | **7.129** | 208.193 | 6782.217 | 5539.021 | 966.60 | **75.26%** | **100.00%** |
| Arch C | 20 | **10551.186** | 10978.964 | 10890.657 | 10879.521 | 240.58 | **93.84%** | **4.62%** |
| Arch D | 20 | **7.129** | 208.077 | 13719.517 | 11178.319 | 795.21 | **79.64%** | **100.00%** |

---

## Table 4: WAN Network Degradation & Clinical Reliability ($N=20$, SLA $\le 500$ ms)
*(Source: `results/csv/exp4_network_stress.csv`)*

| Network Profile | Architecture | WAN Latency (ms) | WAN Bw (Kbps) | Critical Latency (ms) | Avg Latency (ms) | Cloud BW Saving (%) | Critical SLA Rel. (%) |
|---|---|---|---|---|---|---|---|
| Fiber_Normal | Arch A | 100 | 100,000 | **209.899** | 209.747 | 0.00% | **100.00%** |
| Fiber_Normal | Arch C | 100 | 100,000 | **2311.691** | 2536.424 | 77.27% | **22.86%** |
| Fiber_Normal | Arch D | 100 | 100,000 | **7.129** | 2590.063 | 66.58% | **100.00%** |
| WAN_Congested | Arch A | 200 | 1,000 | **419.337** | 418.932 | 0.00% | **100.00%** |
| WAN_Congested | Arch C | 200 | 1,000 | **2560.482** | 2784.159 | 77.27% | **21.48%** |
| WAN_Congested | Arch D | 200 | 1,000 | **7.129** | 2833.101 | 66.58% | **100.00%** |
| WAN_Severe_Bottleneck | Arch A | 400 | 250 | **855.311** | 854.068 | 0.00% | **0.00%** |
| WAN_Severe_Bottleneck | Arch C | 400 | 250 | **3108.709** | 3329.177 | 77.27% | **0.00%** |
| WAN_Severe_Bottleneck | Arch D | 400 | 250 | **7.129** | 3379.659 | 66.58% | **100.00%** |
| WAN_Rural_Degraded | Arch A | 600 | 100 | **1317.167** | 1314.068 | 0.00% | **0.00%** |
| WAN_Rural_Degraded | Arch C | 600 | 100 | **3848.779** | 4059.776 | 77.27% | **0.00%** |
| WAN_Rural_Degraded | Arch D | 600 | 100 | **7.129** | 7651.278 | 66.58% | **100.00%** |
