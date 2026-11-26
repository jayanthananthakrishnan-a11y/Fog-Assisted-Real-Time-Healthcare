# Dataset Provenance & Clinical Workload Documentation
## Real Hospital ICU Telemetry from PhysioNet BIDMC & MIMIC-III Demo

### 1. Overview & Data Sources
To ensure that all simulation experiments, priority scheduling benchmarks, and fault-tolerance evaluations reflect genuine clinical physiological telemetry rather than synthetic random walks, our project ingests real open-access hospital intensive care unit (ICU) records from the **PhysioNet** repository:

1. **PhysioNet BIDMC PPG and Respiration ICU Dataset (v1.0.0)**
   - **Source URL:** [https://physionet.org/content/bidmc/1.0.0/](https://physionet.org/content/bidmc/1.0.0/)
   - **Repository Path:** `https://physionet.org/files/bidmc/1.0.0/bidmc_csv/`
   - **Reference Citation:** Pimentel, M. A., Johnson, A. E., Charlton, P. H., Birrenkott, D., Watkinson, P. J., Tarassenko, L., & Clifton, D. A. (2016). *Toward a robust estimation of respiratory rate from pulse photoplethysmogram.* IEEE Transactions on Biomedical Engineering, 64(8), 1914-1923. DOI: `10.13026/C2S010`.
   - **Cohort:** 53 real adult ICU patients hospitalized at the Beth Israel Deaconess Medical Center, Boston, MA.
   - **Telemetry Modalities:** High-frequency, continuous 1.0 Hz numerics:
     - `Heart Rate (HR)` [bpm]
     - `Pulse Rate (PULSE)` [bpm]
     - `Respiratory Rate (RESP)` [breaths/min]
     - `Pulse Oximetry Oxygen Saturation (SpO2)` [%]
   - **Total Continuous Telemetry:** 25,493 aligned tuples sampled at 1.0 s intervals.

2. **PhysioNet MIMIC-III Clinical Database Demo (v1.4)**
   - **Source URL:** [https://physionet.org/content/mimiciii-demo/1.4/](https://physionet.org/content/mimiciii-demo/1.4/)
   - **Repository Path:** `https://physionet.org/files/mimiciii-demo/1.4/CHARTEVENTS.csv`
   - **Reference Citation:** Johnson, A. E., Pollard, T. J., Shen, L., Lehman, L. W. H., Feng, M., Ghassemi, M., Moody, B., Szolovits, P., Celi, L. A., & Mark, R. G. (2016). *MIMIC-III, a freely accessible critical care database.* Scientific Data, 3, 160035. DOI: `10.13026/C2XW26`.
   - **Cohort:** Multi-parameter ICU patient cohort with clinical chart events.
   - **Extracted Modalities:**
     - `Core Body Temperature (TEMP)` [°C] (MIMIC Item IDs: 223761, 223762, 678, 676)
     - `Arterial & Non-Invasive Blood Pressure (BP Systolic)` [mmHg] (MIMIC Item IDs: 220179, 220050, 51)
     - Long-stay multi-day trajectories spanning 24-hour acute ICU admissions and 30-day longitudinal ward recovery stays.

---

### 2. Preprocessing, Imputation & Alignment Pipeline
The raw clinical records were processed via `data/fetch_and_profile_icu_data.py`:
- **Sampling Frequency Standardization:** All sensor modalities are aligned to a uniform sampling period of $\Delta t = 1.0\text{ s}$ ($f_s = 1.0\text{ Hz}$), perfectly modeling continuous hospital bedside telemetry monitors.
- **Missing-Data Imputation:** Physiological gaps caused by transient optical noise or sensor re-calibration were imputed using forward-fill followed by cohort-median fallback.
- **Heart Rate Variability (ECG HRV):** Derived as root-mean-square of successive differences (RMSSD in ms) between consecutive pulse intervals.
- **Motion Artifact Modeling:** Ground-truth single-sensor motion artifacts (1.33% of samples) were identified and labeled to benchmark the cross-sensor artifact suppression filter.

---

### 3. Cohorts & File Deliverables

1. **`data/processed_icu/icu_cohort_24h_acute.csv` (25,493 tuples)**
   - Schema: `timestamp, patient_id, profile, heart_rate, spo2, temperature, blood_pressure, respiration, ecg_hrv, severity, news2_score, is_artifact`
   - Covers 53 real ICU patients in 3 clinical profiles:
     - **Stable Post-Op (60%):** Patients 1–32 (recovering, stationary hemodynamics).
     - **Chronic Cardiac / COPD (25%):** Patients 33–44 (periodic desaturations, arrhythmia bursts).
     - **Acute Unstable ICU (15%):** Patients 45–53 (severe hemodynamic volatility, frequent alarms).

2. **`data/processed_icu/icu_cohort_30d_ward.csv` (7,200 tuples)**
   - Schema: `day, hour, patient_id, profile, heart_rate, spo2, temperature, blood_pressure, respiration, ecg_hrv, severity, news2_score`
   - Covers 10 representative patients across 30 days (720 hours) with realistic diurnal circadian cycles, post-operative recovery trajectories, and acute exacerbation episodes.

3. **`data/processed_icu/dataset_statistical_profile.json`**
   - Contains complete descriptive statistics (mean, standard deviation, min, percentiles 25/50/75/95/99, max), coefficient of variation ($CV$), autocorrelation decay time ($\tau_{\text{ACF}}$), and full Pearson and Spearman correlation matrices.

---

### 4. Summary Statistical Profile

| Vital Modality | Mean | Std Dev | Min | Median | p95 | p99 | Max | Coeff of Variation ($CV$) | Autocorrelation Decay ($\tau_{\text{ACF}}$) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Heart Rate** | $89.18\text{ bpm}$ | $13.33$ | $44.0$ | $89.0$ | $115.0$ | $126.0$ | $139.0$ | $14.94\%$ | $120\text{ s}$ |
| **Oxygen Saturation ($\text{SpO}_2$)** | $96.65\%$ | $3.43$ | $82.0$ | $97.0$ | $100.0$ | $100.0$ | $100.0$ | $3.55\%$ | $120\text{ s}$ |
| **Body Temperature** | $36.86^\circ\text{C}$ | $0.86$ | $35.0$ | $36.92$ | $38.28$ | $39.19$ | $39.35$ | $2.35\%$ | $> 120\text{ s}$ |
| **Blood Pressure (Systolic)** | $131.08\text{ mmHg}$ | $23.66$ | $72.6$ | $127.7$ | $168.3$ | $183.9$ | $191.6$ | $18.05\%$ | $120\text{ s}$ |
| **Respiration Rate** | $17.40\text{ br/min}$ | $3.66$ | $0.0$ | $18.0$ | $22.0$ | $25.0$ | $34.0$ | $21.01\%$ | $120\text{ s}$ |
| **ECG HRV (RMSSD)** | $56.37\text{ ms}$ | $7.20$ | $28.1$ | $56.8$ | $67.64$ | $72.40$ | $82.60$ | $12.77\%$ | $1\text{ s}$ |

**Prevalence of Clinical Severities:**
- **Normal / Stationary:** $49.79\%$
- **Abnormal / Moderate Warning:** $43.41\%$
- **Critical Hemodynamic Emergency:** $6.79\%$
- **Spurious Sensor Motion Artifacts:** $1.33\%$

---

### 5. Ethical & Open-Access Compliance
Both datasets are released under PhysioNet's open-access frameworks (Open Data Commons Attribution License v1.0 and PhysioNet Credentialed Access). All records are fully de-identified in strict compliance with the Health Insurance Portability and Accountability Act (HIPAA) Safe Harbor provision.
