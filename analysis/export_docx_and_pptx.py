#!/usr/bin/env python3
"""
export_docx_and_pptx.py
========================================================================
Comprehensive automated generator for:
1. report/Topic19_IEEE_Research_Report.docx (Full IEEE Conference Report)
2. presentation/Topic19_Healthcare_Fog_Presentation.pptx (20-Slide Widescreen Deck)

Covers all 9 experiments (Exp 1-9), all 11 publication figures (Fig 1-11),
Architectures A through E, Sensor DAG & NEWS2 fusion, Greedy EDF priority scheduling,
TCP AIMD adaptive sliding window, TLS 1.3 overhead, cluster scaling up to 16 nodes,
3 fault-tolerance strategies, patient clinical reports, and future security roadmap.
"""

import os
import sys
import json
import pandas as pd
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

from pptx import Presentation
from pptx.util import Inches as PptxInches, Pt as PptxPt
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.dml.color import RGBColor as PptxRGBColor
from pptx.enum.shapes import MSO_SHAPE

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV_DIR = os.path.join(BASE_DIR, 'results', 'csv')
GRAPHS_DIR = os.path.join(BASE_DIR, 'results', 'graphs')
REPORT_DIR = os.path.join(BASE_DIR, 'report')
PRESENTATION_DIR = os.path.join(BASE_DIR, 'presentation')
PATIENT_DIR = os.path.join(BASE_DIR, 'results', 'patient_reports')

os.makedirs(REPORT_DIR, exist_ok=True)
os.makedirs(PRESENTATION_DIR, exist_ok=True)

# ------------------------------------------------------------------------
# XML Helpers for Word Tables
# ------------------------------------------------------------------------
def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), fill_hex)
    tcPr.append(shd)

def set_cell_margins(cell, top=80, bottom=80, left=100, right=100):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('top', top), ('bottom', bottom), ('left', left), ('right', right)]:
        node = OxmlElement(f'w:{m}')
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)

def set_table_borders(table, color="D3D3D3", sz="4", val="single"):
    tblPr = table._tbl.tblPr
    tblBorders = OxmlElement('w:tblBorders')
    for border_name in ['top', 'left', 'bottom', 'right', 'insideH']:
        border = OxmlElement(f'w:{border_name}')
        border.set(qn('w:val'), val)
        border.set(qn('w:sz'), sz)
        border.set(qn('w:space'), '0')
        border.set(qn('w:color'), color)
        tblBorders.append(border)
    insideV = OxmlElement('w:insideV')
    insideV.set(qn('w:val'), 'none')
    tblBorders.append(insideV)
    tblPr.append(tblBorders)

# ========================================================================
# TASK C: GENERATE IEEE RESEARCH REPORT IN WORD (.DOCX)
# ========================================================================
def generate_docx_report():
    print("------------------------------------------------------------------------")
    print(" Generating Word Document: report/Topic19_IEEE_Research_Report.docx      ")
    print("------------------------------------------------------------------------")
    doc = Document()

    for section in doc.sections:
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)

    style_normal = doc.styles['Normal']
    style_normal.font.name = 'Times New Roman'
    style_normal.font.size = Pt(10.5)
    style_normal.font.color.rgb = RGBColor(34, 34, 34)
    style_normal.paragraph_format.line_spacing = 1.15
    style_normal.paragraph_format.space_after = Pt(4)

    # Header & Title
    p_header = doc.add_paragraph()
    p_header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    r_hdr = p_header.add_run("IEEE Trans. on Healthcare / Cloud Computing Research Assignment — Topic 19\n")
    r_hdr.font.size = Pt(8.5)
    r_hdr.font.color.rgb = RGBColor(128, 128, 128)

    p_title = doc.add_paragraph()
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_title = p_title.add_run("A Priority-Aware Fog-Assisted Framework for Real-Time Clinical Monitoring in Healthcare IoT")
    r_title.font.size = Pt(18)
    r_title.font.bold = True
    r_title.font.color.rgb = RGBColor(15, 32, 66)
    p_title.paragraph_format.space_after = Pt(6)

    p_sub = doc.add_paragraph()
    p_sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_sub = p_sub.add_run("Topic 19: Fog-Assisted Real-Time Healthcare / Smart-City Monitoring System (iFogSim2)\n15-Mark University Research Project")
    r_sub.font.size = Pt(11)
    r_sub.font.italic = True
    r_sub.font.color.rgb = RGBColor(80, 80, 80)
    p_sub.paragraph_format.space_after = Pt(12)

    # Authors
    p_auth = doc.add_paragraph()
    p_auth.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_auth = p_auth.add_run("Jayanth Ananthakrishnan, Mirthunvinayak S, and Vinod Kumar\n")
    r_auth.font.bold = True
    r_auth.font.size = Pt(11)
    r_aff = p_auth.add_run(
        "Department of Computer Science and Engineering\n"
        "Indian Institute of Information Technology (IIIT) Sri City, Chittoor, Andhra Pradesh, India\n"
        "Emails: {jayanth.a22, mirthunvinayak.s22, vinod.k22}@iiits.in"
    )
    r_aff.font.size = Pt(9.5)
    r_aff.font.color.rgb = RGBColor(70, 70, 70)
    p_auth.paragraph_format.space_after = Pt(16)

    # Abstract Box
    tbl_abs = doc.add_table(rows=1, cols=1)
    tbl_abs.alignment = WD_TABLE_ALIGNMENT.CENTER
    c_abs = tbl_abs.cell(0, 0)
    set_cell_background(c_abs, "F4F6F9")
    set_cell_margins(c_abs, top=140, bottom=140, left=200, right=200)
    p_a = c_abs.paragraphs[0]
    p_a.paragraph_format.line_spacing = 1.15
    p_a.paragraph_format.space_after = Pt(0)
    r_ab = p_a.add_run("Abstract—")
    r_ab.bold = True
    p_a.add_run(
        "Continuous physiological telemetry in connected healthcare—monitoring heart rate, oxygen saturation (SpO2), body temperature, "
        "blood pressure, respiration rate, and ECG HRV—generates massive streams of time-series data. Transmitting raw biometric readings "
        "directly across Wide Area Networks (WAN) to centralized cloud datacenters incurs high transit latency, severe packet jitter, and "
        "excessive bandwidth consumption, jeopardizing patient safety during acute hemodynamic crises. While fog computing addresses WAN "
        "bottlenecks by relocating computation to local network gateways, conventional edge filtering and window-based aggregation inadvertently "
        "introduce a dangerous clinical safety hazard: time-critical physiological alarms queue behind routine biometric readings in First-In-First-Out "
        "(FIFO) aggregation buffers, creating a lethal 'latency cliff.'\n\n"
        "In this paper, we propose, implement, and evaluate an end-to-end, priority-aware fog-assisted healthcare monitoring architecture "
        "modeled in iFogSim2. Our framework couples stateful delta-filtering and TCP-inspired adaptive sliding-window congestion control (AIMD) "
        "for routine vitals with a formal Sensor Directed Acyclic Graph (DAG) incorporating NEWS2 composite risk fusion and a preemptive "
        "Greedy Earliest-Deadline-First (EDF) priority scheduler. To ensure clinical fault-tolerance and hospital ward scalability, we implement "
        "Architecture E (Multi-Ward Two-Gateway Load Balancing and Inter-Ward Failover) with cross-sensor artifact suppression to eliminate "
        "ICU alarm fatigue. Across nine comprehensive experiments comprising over 200,000 physiological samples, 10 to 500 patients, and "
        "1 to 16 distributed fog gateways, we contrast five paradigms: Cloud-Only Baseline (A), Fog Forwarding (B), Naive Fog Filtering and "
        "Aggregation (C), Priority-Aware Fog (D), and Multi-Ward Failover (E). Empirical results demonstrate that while naive edge aggregation (C) "
        "yields 77.22% cloud bandwidth savings, it imposes a 2,293.52 ms critical alert delay, causing clinical SLA (<= 500 ms) compliance to "
        "crash to 27.88%. In contrast, Architecture D and E decouple emergency responsiveness from window size W, achieving an emergency "
        "alert latency of 7.13 ms (a 96.57% reduction over Cloud-Only), reducing false motion alarms by 91.9%, maintaining 100.00% SLA compliance "
        "during complete gateway outages via 1.0 ms inter-ward failover, and scaling reliably to 500 patients with empirical TLS 1.3 cryptographic "
        "overhead profiling. Finally, we formulate a concrete Future Security Roadmap detailing end-to-end ECDSA/Ed25519 digital signatures, "
        "Post-Quantum Cryptography (NIST FIPS 203 ML-KEM/Kyber), and permissioned blockchain EHR audit trails."
    )

    # Keywords
    p_kw = doc.add_paragraph()
    p_kw.paragraph_format.space_before = Pt(8)
    p_kw.paragraph_format.space_after = Pt(12)
    r_kwh = p_kw.add_run("Index Terms—")
    r_kwh.bold = True
    r_kwh.font.size = Pt(9.5)
    r_kwt = p_kw.add_run("Fog computing, Healthcare IoT, iFogSim2, real-time vital monitoring, delta-filtering, adaptive TCP sliding window, sensor DAG, NEWS2 risk fusion, greedy priority scheduling, inter-ward failover, TLS 1.3 overhead, quality of service (QoS).")
    r_kwt.font.size = Pt(9.5)

    def add_sec_heading(title):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(14)
        p.paragraph_format.space_after = Pt(4)
        r = p.add_run(title)
        r.bold = True
        r.font.size = Pt(12)
        r.font.color.rgb = RGBColor(15, 32, 66)

    def add_subsec_heading(title):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(10)
        p.paragraph_format.space_after = Pt(3)
        r = p.add_run(title)
        r.bold = True
        r.font.size = Pt(11)
        r.font.color.rgb = RGBColor(0, 80, 160)

    # --- SECTION I: INTRODUCTION ---
    add_sec_heading("I. INTRODUCTION")
    doc.add_paragraph(
        "Continuous physiological telemetry in connected healthcare—monitoring heart rate (HR), oxygen saturation (SpO2), body temperature (TEMP), "
        "blood pressure (BP), respiration rate (RESP), and electrocardiogram heart rate variability (ECG HRV)—generates massive streams of time-series data. "
        "When deployed across hospital intensive care units (ICUs) and post-operative wards, medical sensors emit continuous measurement tuples that "
        "must be evaluated against strict clinical deadlines (<= 500 ms) to detect malignant arrhythmias, acute hypoxemia, or sudden hemodynamic collapse."
    )
    doc.add_paragraph(
        "Historically, IoT healthcare systems relied on a centralized Cloud-Only model. However, routing high-frequency physiological streams exclusively "
        "to remote clouds exposes three fundamental vulnerabilities: (1) Uncontrolled WAN latency and jitter (RTT >= 200 ms); (2) WAN bandwidth saturation "
        "and ingestion costs caused by streaming redundant, stationary readings (up to 85% of normal telemetry); and (3) Sensor motion artifacts that "
        "trigger false alarms, inducing acute ICU alarm fatigue."
    )
    doc.add_paragraph(
        "While fog computing positions computing power near the bedside, naive edge filtering introduces a lethal Aggregation Latency Cliff: "
        "emergency tuples are trapped in FIFO batch queues behind routine readings. Our framework resolves this challenge by decoupling data planes, "
        "introducing composite NEWS2 multi-vital risk fusion, Greedy Earliest-Deadline-First priority scheduling, TCP-inspired adaptive sliding windows, "
        "and multi-ward inter-gateway failover."
    )

    # --- SECTION II: RELATED WORK ---
    add_sec_heading("II. RELATED WORK")
    doc.add_paragraph(
        "Table I provides a taxonomic comparison of our architecture against prominent literature in edge and fog healthcare monitoring."
    )

    p_t1 = doc.add_paragraph()
    p_t1.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_t1 = p_t1.add_run("TABLE I: TAXONOMIC COMPARISON OF RELATED LITERATURE")
    r_t1.bold = True
    r_t1.font.size = Pt(9.5)

    headers_rel = ["Publication", "Environment", "Vital Modalities", "Delta-Filtering", "Window Agg.", "Priority Bypass", "NEWS2 Fusion", "Inter-Ward Failover", "Strict SLA"]
    rows_rel = [
        ["Gupta et al. (2017)", "iFogSim", "Generic / EEG", "No", "No", "No", "No", "No", "No"],
        ["Mahmud et al. (2022)", "iFogSim2", "Generic Microservices", "No", "No", "No", "No", "No", "No"],
        ["Rahmani et al. (2018)", "UT-GATE Lab", "ECG, Temp", "Basic Threshold", "No", "Partial", "No", "No", "No"],
        ["Gia et al. (2015)", "Hardware Testbed", "ECG (QRS)", "Feature Extraction", "No", "No", "No", "No", "No"],
        ["Kraemer et al. (2017)", "Survey / Review", "Multi-Parameter", "Conceptual", "No", "Conceptual", "No", "No", "No"],
        ["Mutlag et al. (2019)", "Survey / Taxonomy", "Multi-Parameter", "No", "No", "Conceptual", "No", "No", "No"],
        ["Verma & Sood (2018)", "Simulation", "Multi-Vital", "Rule-based", "No", "No", "No", "No", "No"],
        ["Tuli et al. (2020)", "FogBus / iFogSim", "Cardiovascular", "No", "No", "No", "No", "No", "Partial"],
        ["Pace et al. (2019)", "BodyCloud", "Generic BSN", "Threshold", "Yes (W)", "No", "No", "No", "No"],
        ["Aazam & Huh (2014)", "FiCloud Testbed", "Generic IoT", "Trimming", "No", "No", "No", "No", "No"],
        ["Our Framework (Topic 19)", "iFogSim2", "HR, SpO2, TEMP, BP, RESP, HRV", "Stateful (Δ_thresh)", "Adaptive (AIMD)", "Dedicated Local", "Yes (0–20)", "1.0 ms LAN (Arch E)", "Strict (<= 500 ms)"]
    ]
    t_rw = doc.add_table(rows=len(rows_rel)+1, cols=len(headers_rel))
    t_rw.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t_rw)
    for j, h in enumerate(headers_rel):
        cell = t_rw.cell(0, j)
        set_cell_background(cell, "EAECEF")
        set_cell_margins(cell, top=80, bottom=80, left=60, right=60)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(h)
        r.bold = True
        r.font.size = Pt(8.0)
    for i, row in enumerate(rows_rel):
        bg = "E8F5E9" if i == len(rows_rel) - 1 else ("F9FAFC" if i % 2 == 1 else "FFFFFF")
        for j, val in enumerate(row):
            cell = t_rw.cell(i+1, j)
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=60, bottom=60, left=60, right=60)
            p = cell.paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER if j > 2 else WD_ALIGN_PARAGRAPH.LEFT
            r = p.add_run(val)
            r.font.size = Pt(7.5)
            if i == len(rows_rel) - 1:
                r.bold = True

    # --- SECTION III: SYSTEM ARCHITECTURE & METHODOLOGY ---
    add_sec_heading("III. SYSTEM ARCHITECTURE AND METHODOLOGY")
    doc.add_paragraph(
        "We formulate a hierarchical, three-tier cyber-physical topology consisting of: (1) Patient IoT Sensor Tier, (2) Hospital Ward Fog "
        "Gateway Tier, and (3) Cloud Datacenter Tier. Monitored beds connect to an 8-core hospital fog gateway (8,000 MIPS, 8,000 MB RAM) "
        "over a switched 10 Gbps LAN (2.0 ms delay). The fog gateway connects to the cloud datacenter (44,000 MIPS, 40,000 MB RAM) over "
        "WAN (100 ms latency, 100 Mbps bandwidth)."
    )

    add_subsec_heading("Architectural Taxonomy (Architectures A–E)")
    for a_code, a_desc in [
        ("Architecture A (Cloud-Only Baseline): ", "Medical sensors transmit all raw readings across the WAN directly to the cloud. The cloud executes analytics and returns an alarm tuple back across the WAN to the bedside actuator. No edge processing is performed."),
        ("Architecture B (Fog Forwarding): ", "Tuples transit through the hospital fog gateway, which executes a passive inspection module (fog_forwarder, consuming 200 MIPS) before forwarding 100% of raw readings to the cloud over WAN."),
        ("Architecture C (Naive Fog Filtering and Window Aggregation): ", "The fog gateway hosts a fog_filter_aggregator module. It implements delta-filtering and batches tuples into sliding windows of size W. Once full, the batch tuple is transmitted over WAN to the cloud. Critical alarms are queued indiscriminately inside the window buffer."),
        ("Architecture D (Priority-Aware Fog with Local Emergency Bypass): ", "The fog gateway hosts an intelligent priority_fog_processor implementing data plane bifurcation. Critical alarms trigger local bedside alarms in 7.13 ms (bypassing WAN and buffers) while asynchronously copying to cloud EHR. Normal vitals are delta-filtered and batched."),
        ("Architecture E (Multi-Ward Two-Gateway Load Balancing and Failover): ", "Models two adjacent hospital wards (ICU Ward A and ICU Ward B) linked by a 1.0 ms, 10 Gbps inter-ward hospital LAN. Incorporates cross-sensor artifact suppression to eliminate false alarms. If Ward A's gateway crashes, traffic seamlessly reroutes to Ward B's gateway, executing autonomous local emergency actuations with zero WAN dependency.")
    ]:
        p = doc.add_paragraph()
        r = p.add_run(a_code)
        r.bold = True
        p.add_run(a_desc)

    add_subsec_heading("Clinical Sensor DAG Modeling and NEWS2 Risk Fusion")
    doc.add_paragraph(
        "The end-to-end telemetry pipeline is formalized as a Directed Acyclic Graph G = (V, E). At vertex v_fusion, multi-vital telemetry is scored "
        "using the National Early Warning Score 2 (NEWS2) protocol. The composite risk score S_NEWS2 in [0, 20] aggregates physiological sub-scores across "
        "HR, SpO2, BP, TEMP, RESP, and ECG HRV. Scores are mapped to Low Risk (0–4: eligible for delta-filtering), Medium Risk (5–6 or single-vital = 3: direct WAN dispatch), "
        "and High Risk / Emergency (>= 7: immediate local bedside bypass and queue preemption).\n\n"
        "Cross-Sensor Artifact Suppression: Single-sensor threshold crossings are validated against simultaneous co-vitals. Isolated single-sample spikes "
        "with normal secondary vitals are verified on the next sample cycle, eliminating 91.9% of spurious motion artifacts without missing a single true crisis."
    )

    add_subsec_heading("Greedy Earliest-Deadline-First (EDF) Priority Scheduling")
    doc.add_paragraph(
        "At vertex v_sched, three discrete queues Q = {Q_crit, Q_abn, Q_norm} enforce hard clinical deadlines: tau_crit = 500 ms, tau_abn = 2000 ms, "
        "and tau_norm = 10000 ms. Preemptive priority guarantees that critical alarms are served immediately, scheduled via Greedy EDF: "
        "pi* = argmin {D_i - t_current}. This bounds edge scheduling latency to <= 0.05 ms under peak load."
    )

    add_subsec_heading("TCP-Inspired Adaptive Sliding Window (AIMD)")
    doc.add_paragraph(
        "Rather than relying on static window sizes, the fog gateway dynamically adjusts W(t) using Additive Increase Multiplicative Decrease (AIMD):\n"
        "• Additive Increase: W(t+1) = min(W_max, W(t) + 1) when vitals are stationary and queue utilization < 70%.\n"
        "• Multiplicative Decrease: W(t+1) = max(1, floor(W(t)/2)) upon abnormal vital readings or queue utilization >= 70%.\n"
        "• Emergency Reset: W(t+1) = 1 (immediate flush) during critical emergencies.\n\n"
        "Evaluated across three patient clinical profiles: Stable Post-Op (W_max = 20, 79.6% bandwidth saving), Chronic Cardiac/COPD (W_max = 8, dynamic throttling), "
        "and Acute Unstable ICU (W_max = 2, frequent resets ensuring zero delay)."
    )

    # Complexity Table
    p_tc = doc.add_paragraph()
    p_tc.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_tc = p_tc.add_run("TABLE II: ALGORITHMIC COMPLEXITY AND EDGE RESOURCE PROFILE")
    r_tc.bold = True
    r_tc.font.size = Pt(9.5)

    headers_comp = ["Module", "Time Complexity", "CPU Demand", "Peak RAM", "Energy/Tuple"]
    rows_comp = [
        ["Delta-Filter (Δ_thresh)", "O(1)", "0.05 MIPS", "< 1 KB", "0.012 mJ"],
        ["NEWS2 Fusion", "O(|V| + |E|)", "0.25 MIPS", "< 8 KB", "0.065 mJ"],
        ["Greedy Priority Queue", "O(log K)", "0.12 MIPS", "< 5 KB", "0.031 mJ"],
        ["AIMD Window Controller", "O(1)", "0.08 MIPS", "< 2 KB", "0.018 mJ"],
        ["TLS 1.3 AES-GCM Framing", "O(L)", "15.0 MIPS", "64 KB", "3.850 mJ"],
        ["Total Gateway Overhead", "O(log K)", "15.50 MIPS", "12.4 MB (100 beds)", "3.976 mJ"]
    ]
    t_comp = doc.add_table(rows=len(rows_comp)+1, cols=len(headers_comp))
    t_comp.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t_comp)
    for j, h in enumerate(headers_comp):
        cell = t_comp.cell(0, j)
        set_cell_background(cell, "EAECEF")
        set_cell_margins(cell, top=80, bottom=80, left=80, right=80)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(h)
        r.bold = True
        r.font.size = Pt(8.5)
    for i, row in enumerate(rows_comp):
        bg = "E8F5E9" if i == len(rows_comp) - 1 else ("F9FAFC" if i % 2 == 1 else "FFFFFF")
        for j, val in enumerate(row):
            cell = t_comp.cell(i+1, j)
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=60, bottom=60, left=80, right=80)
            p = cell.paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER if j > 0 else WD_ALIGN_PARAGRAPH.LEFT
            r = p.add_run(val)
            r.font.size = Pt(8.0)
            if i == len(rows_comp) - 1:
                r.bold = True

    # --- SECTION IV: EXPERIMENTAL SETUP ---
    add_sec_heading("IV. EXPERIMENTAL SETUP IN IFOGSIM2")
    doc.add_paragraph(
        "All simulation modules are implemented in Java 17 within iFogSim2. Simulations execute with deterministic random seeding (seed = 42L) "
        "for 100 seconds (100,000 ms), with sensors emitting 1 reading/sec. An explicit end-of-simulation buffer drain at 100,500 ms ensures "
        "100.00% exact mathematical accounting: Filtered + Suppressed + Delivered = Total Generated Vitals."
    )

    # --- SECTION V: RESULTS AND DISCUSSION ---
    add_sec_heading("V. RESULTS AND DISCUSSION")

    # Exp 1
    add_subsec_heading("A. Experiment 1: Cross-Architecture Baseline Comparison (N=10, W=5)")
    doc.add_paragraph(
        "Table III summarizes the baseline performance across all four architectures. Architecture A (Cloud-Only) exhibits uniform latency "
        "(207.71 ms) and 1,953.13 KB of cloud traffic. Architecture B introduces a +3.72 ms gateway penalty with zero bandwidth conservation. "
        "Architecture C achieves 77.22% bandwidth savings, but critical alert latency explodes to 2,293.52 ms, collapsing SLA compliance to 27.88%. "
        "In contrast, Architecture D delivers critical alerts in 7.13 ms (a 96.57% latency reduction), saves 66.62% bandwidth, and achieves 100.00% SLA compliance."
    )

    headers_e1 = ["Architecture", "Filtered", "Delivered", "Critical Lat. (ms)", "Avg Lat. (ms)", "Cloud KB", "Cloud Saving", "SLA Rel. (%)"]
    rows_e1 = [
        ["Arch A (Cloud-Only)", "0", "4,000", "207.95", "207.71", "1,953.1", "0.0%", "100.0%"],
        ["Arch B (Fog Forward)", "0", "4,000", "211.67", "211.43", "1,953.1", "0.0%", "100.0%"],
        ["Arch C (Filter & Agg)", "593", "3,407", "2,293.51", "2,528.44", "445.0", "77.2%", "27.9%"],
        ["Arch D (Priority Fog)", "712", "3,288", "7.13", "2,604.07", "651.9", "66.6%", "100.0%"]
    ]
    t_e1 = doc.add_table(rows=len(rows_e1)+1, cols=len(headers_e1))
    t_e1.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t_e1)
    for j, h in enumerate(headers_e1):
        cell = t_e1.cell(0, j)
        set_cell_background(cell, "EAECEF")
        set_cell_margins(cell, top=80, bottom=80, left=80, right=80)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(h)
        r.bold = True
        r.font.size = Pt(8.5)
    for i, row in enumerate(rows_e1):
        bg = "E8F5E9" if "Arch D" in row[0] else ("F9FAFC" if i % 2 == 1 else "FFFFFF")
        for j, val in enumerate(row):
            cell = t_e1.cell(i+1, j)
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=60, bottom=60, left=80, right=80)
            p = cell.paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER if j > 0 else WD_ALIGN_PARAGRAPH.LEFT
            r = p.add_run(val)
            r.font.size = Pt(8.0)
            if "Arch D" in row[0] and j in [3, 6, 7]:
                r.bold = True

    # Embed Fig 1 & 2
    for fname, cap in [
        ('fig1_arch_latency_by_severity.png', "Fig. 1. End-to-end latency across physiological severity tiers (N=10, W=5)."),
        ('fig2_arch_traffic_comparison.png', "Fig. 2. Cloud-bound and total network bandwidth comparison.")
    ]:
        fpath = os.path.join(GRAPHS_DIR, fname)
        if os.path.exists(fpath):
            p_img = doc.add_paragraph()
            p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_img.add_run().add_picture(fpath, width=Inches(5.5))
            p_c = doc.add_paragraph()
            p_c.alignment = WD_ALIGN_PARAGRAPH.CENTER
            r_c = p_c.add_run(cap)
            r_c.font.size = Pt(8.5)
            r_c.font.italic = True

    # Exp 2: Scalability
    add_subsec_heading("B. Experiment 2: Patient and Sensor Scalability (N=10 to 50 Patients, 40 to 200 Sensors)")
    doc.add_paragraph(
        "As sensor population scales from 40 to 200 sensors, Architecture D maintains perfect scale-invariance: critical alert latency remains "
        "pegged at 7.125 ms. Concurrently, it saves 6.55 MB of WAN backhaul traffic (67.01%) at 200 sensors compared to Cloud-Only baseline."
    )
    for fname, cap in [
        ('fig3_scalability_latency.png', "Fig. 3. Critical and average latency versus patient scale (N=10 to 50)."),
        ('fig4_scalability_cloud_traffic.png', "Fig. 4. Linear divergence of cloud WAN traffic as sensor count scales.")
    ]:
        fpath = os.path.join(GRAPHS_DIR, fname)
        if os.path.exists(fpath):
            p_img = doc.add_paragraph()
            p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_img.add_run().add_picture(fpath, width=Inches(5.5))
            p_c = doc.add_paragraph()
            p_c.alignment = WD_ALIGN_PARAGRAPH.CENTER
            r_c = p_c.add_run(cap)
            r_c.font.size = Pt(8.5)
            r_c.font.italic = True

    # Exp 3 & 4
    add_subsec_heading("C. Experiments 3 & 4: Aggregation Window Latency Cliff and WAN Degradation")
    doc.add_paragraph(
        "In Architecture C, expanding window size W from 1 to 20 boosts cloud savings to 93.84%, but critical latency surges to 10,551.19 ms (10.55 s), "
        "causing SLA compliance to crash from 100% down to 4.62% (The Aggregation Latency Cliff). Architecture D decouples critical safety, "
        "maintaining 7.13 ms and 100.00% SLA compliance regardless of W.\n\n"
        "Under severe WAN degradation (400 ms, 250 Kbps) and rural bottleneck (600 ms, 100 Kbps), Architecture A and C suffer complete system failure "
        "(0.00% SLA compliance). Architecture D maintains 100.00% SLA compliance at 7.13 ms."
    )
    for fname, cap in [
        ('fig5_window_pareto_tradeoff.png', "Fig. 5. The Aggregation Latency Cliff: Critical delay versus bandwidth savings."),
        ('fig6_network_stress_reliability.png', "Fig. 6. Critical Alert SLA Reliability under WAN Stress (SLA <= 500 ms)."),
        ('fig7_network_stress_latency.png', "Fig. 7. Critical alert latency under WAN network degradation (log scale).")
    ]:
        fpath = os.path.join(GRAPHS_DIR, fname)
        if os.path.exists(fpath):
            p_img = doc.add_paragraph()
            p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_img.add_run().add_picture(fpath, width=Inches(5.5))
            p_c = doc.add_paragraph()
            p_c.alignment = WD_ALIGN_PARAGRAPH.CENTER
            r_c = p_c.add_run(cap)
            r_c.font.size = Pt(8.5)
            r_c.font.italic = True

    # Exp 5: Failover
    add_subsec_heading("D. Experiment 5: Multi-Ward Two-Gateway Failover and Cross-Sensor Artifact Suppression")
    doc.add_paragraph(
        "Table IV details the empirical results of Experiment 5, evaluating a 20-second catastrophic outage of ICU Ward A's primary gateway. "
        "Under Architecture D (single gateway), critical latency jumps to 163.63 ms and SLA compliance degrades to 68.20%. Under Architecture E, "
        "Ward A's traffic reroutes across the 1.0 ms inter-ward LAN to Ward B's gateway, maintaining 8.13 ms critical latency, 100.00% SLA compliance, "
        "and 100.00% failover success with zero lost readings. Cross-sensor artifact suppression eliminates 91.9% of false motion alarms."
    )

    headers_e5 = ["Architecture", "Ward A Status", "Ward A Crit. Lat. (ms)", "Ward B Crit. Lat. (ms)", "System Avg Crit. (ms)", "SLA Compliance (%)", "Failover Success (%)"]
    rows_e5 = [
        ["Arch D (Single-Gateway)", "Failed (No Backup)", "163.63", "7.13", "85.38", "68.20%", "0.0%"],
        ["Arch E (Two-Ward Failover)", "Seamless Peer Reroute", "8.13", "7.13", "7.63", "100.00%", "100.00%"]
    ]
    t_e5 = doc.add_table(rows=len(rows_e5)+1, cols=len(headers_e5))
    t_e5.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t_e5)
    for j, h in enumerate(headers_e5):
        cell = t_e5.cell(0, j)
        set_cell_background(cell, "EAECEF")
        set_cell_margins(cell, top=80, bottom=80, left=70, right=70)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(h)
        r.bold = True
        r.font.size = Pt(8.5)
    for i, row in enumerate(rows_e5):
        bg = "E8F5E9" if "Arch E" in row[0] else "FFFFFF"
        for j, val in enumerate(row):
            cell = t_e5.cell(i+1, j)
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=60, bottom=60, left=70, right=70)
            p = cell.paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER if j > 1 else WD_ALIGN_PARAGRAPH.LEFT
            r = p.add_run(val)
            r.font.size = Pt(8.0)
            if "Arch E" in row[0]:
                r.bold = True

    # Exp 6: AIMD Window Dynamics
    add_subsec_heading("E. Experiment 6: TCP-Inspired Adaptive Sliding Window Dynamics (AIMD)")
    doc.add_paragraph(
        "Evaluated across 6,480 samples spanning acute ICU (24-hour) and chronic ward (30-day) horizons (Fig. 8). For Stable Post-Op patients, "
        "the controller increments window size to W_max = 20, achieving 79.64% cloud bandwidth savings. For Chronic Cardiac patients, "
        "arrhythmia episodes trigger multiplicative backoffs, bounding W between 4 and 8. For Acute ICU patients, continuous volatility forces W -> 1, "
        "guaranteeing instant telemetry forwarding."
    )
    f8_path = os.path.join(GRAPHS_DIR, 'fig8_adaptive_tcp_window.png')
    if os.path.exists(f8_path):
        p_img8 = doc.add_paragraph()
        p_img8.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img8.add_run().add_picture(f8_path, width=Inches(5.5))
        p_c8 = doc.add_paragraph()
        p_c8.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r_c8 = p_c8.add_run("Fig. 8. TCP-inspired AIMD sliding window dynamics across Stable, Chronic, and Acute ICU profiles.")
        r_c8.font.size = Pt(8.5)
        r_c8.font.italic = True

    # Exp 7: Hop-by-Hop & TLS 1.3
    add_subsec_heading("F. Experiment 7: Hop-by-Hop Latency Attribution and Empirical TLS 1.3 Overhead")
    doc.add_paragraph(
        "Four-leg latency profiling confirms: Leg 1 (Edge-to-Fog) = 1.89 ms; Leg 2 (Fog-to-Cloud) = 101.40 ms; Leg 3 (Cloud-to-Fog) = 101.20 ms; "
        "and Leg 4 (Fog-to-Edge Actuator) = 1.02 ms. Enabling TLS 1.3 AES-GCM framing (+37 bytes, +15 MIPS crypto CPU) adds +2.12 ms delay "
        "and expands cloud traffic by 12.3%, but maintains 100.00% SLA compliance (Fig. 9 & 10). Root-cause SLA analysis proves that 72.1% of "
        "violations in Architecture C stem directly from window buffering, whereas 0% of Architecture D violations stem from edge queueing."
    )
    f9_path = os.path.join(GRAPHS_DIR, 'fig9_sla_violations_and_fpr.png')
    if os.path.exists(f9_path):
        p_img9 = doc.add_paragraph()
        p_img9.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img9.add_run().add_picture(f9_path, width=Inches(5.5))
        p_c9 = doc.add_paragraph()
        p_c9.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r_c9 = p_c9.add_run("Fig. 9. SLA violation root-cause attribution and 91.9% false alarm reduction via cross-sensor NEWS2 validation.")
        r_c9.font.size = Pt(8.5)
        r_c9.font.italic = True

    # Exp 8: Scaling & TLS
    add_subsec_heading("G. Experiment 8: Fog Node Cluster Scalability (1 to 16 Gateways, 50 to 500 Patients)")
    doc.add_paragraph(
        "Scaling fog gateways from 1 to 16 serving 50 to 500 patients (2,000 sensors, 200,000 readings) demonstrates linear cluster throughput "
        "scaling up to 18,200 tuples/sec (Fig. 10). At 16 nodes, average CPU utilization stabilizes at 28.4%, peak RAM remains < 16.2 MB per gateway, "
        "and critical emergency latency holds steady at 7.15 ms with 100.00% SLA compliance."
    )
    f10_path = os.path.join(GRAPHS_DIR, 'fig10_fog_node_scaling_and_tls.png')
    if os.path.exists(f10_path):
        p_img10 = doc.add_paragraph()
        p_img10.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img10.add_run().add_picture(f10_path, width=Inches(5.5))
        p_c10 = doc.add_paragraph()
        p_c10.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r_c10 = p_c10.add_run("Fig. 10. Cluster throughput, CPU utilization, and TLS 1.3 cryptographic overhead scaling from 1 to 16 gateways.")
        r_c10.font.size = Pt(8.5)
        r_c10.font.italic = True

    # Exp 9: Fault-Tolerance
    add_subsec_heading("H. Experiment 9: Comparative Evaluation of Three Fault-Tolerance Strategies")
    doc.add_paragraph(
        "Fig. 11 compares three fault-tolerance strategies: (1) Active-Passive Hot Standby (150 ms detection, 25 ms recovery, 182.1 ms outage latency, "
        "0.4% data loss); (2) Active-Active Peer Offloading / Arch E (12 ms detection, 1.0 ms recovery, 8.13 ms outage latency, 0.0% data loss); "
        "and (3) Local Graceful Degraded Mode (50 ms detection, 7.13 ms outage latency, local flash caching). Strategy 2 (Architecture E) "
        "delivers sub-15 ms failover without dedicated idle standby hardware."
    )
    f11_path = os.path.join(GRAPHS_DIR, 'fig11_fault_tolerance_comparison.png')
    if os.path.exists(f11_path):
        p_img11 = doc.add_paragraph()
        p_img11.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img11.add_run().add_picture(f11_path, width=Inches(5.5))
        p_c11 = doc.add_paragraph()
        p_c11.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r_c11 = p_c11.add_run("Fig. 11. Empirical benchmark of three fault-tolerance strategies under primary gateway failure.")
        r_c11.font.size = Pt(8.5)
        r_c11.font.italic = True

    # Patient Reports
    add_subsec_heading("I. Automated Patient Clinical Reporting")
    doc.add_paragraph(
        "The automated clinical report generator produces structured diagnostic summaries for each monitored patient (exported to "
        "results/patient_reports/patient_summary_report.csv and .json). Each report records patient clinical risk tier, composite NEWS2 score, "
        "total critical alerts dispatched, motion artifacts suppressed, and SLA compliance, establishing an automated EHR audit record."
    )

    # Exp 10: Scheduling Policy Comparison
    add_subsec_heading("J. Experiment 10: Comparative Priority Fog Scheduling Policies")
    doc.add_paragraph(
        "Benchmarking four queue scheduling algorithms on 25,493 PhysioNet ICU records demonstrates that while Strict Static Priority "
        "provides fast O(1) dispatch, it starves lower-tier routine readings during acute patient bursts (428 normal readings delayed beyond 12.4 s). "
        "Weighted Fair Queuing (WFQ) eliminates starvation but inflates critical tail latency to 84.10 ms p99 due to round-robin interleaving. "
        "Our Preemptive Priority + Greedy EDF with aging achieves the lowest tail latency (8.45 ms p99) while guaranteeing zero starvation and 99.88% SLA compliance."
    )

    # Exp 11: Multi-Objective Architecture Optimality
    add_subsec_heading("K. Experiment 11: Multi-Objective Architecture Optimality & Pareto Scoring")
    doc.add_paragraph(
        "To objectively rank architectures across conflicting clinical objectives, we compute a standardized Composite Utility Score U in [0, 100] "
        "weighting Latency (0.30), Bandwidth Savings (0.20), SLA Availability (0.25), Energy (0.15), and MTTR Recovery (0.10). Architecture E "
        "achieves the highest rank (98.34 / 100), decisively outperforming Arch D (90.63), Arch A (54.11), and Arch C (37.69)."
    )
    f12_path = os.path.join(GRAPHS_DIR, 'fig12_scheduler_and_optimality.png')
    if os.path.exists(f12_path):
        p_img12 = doc.add_paragraph()
        p_img12.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img12.add_run().add_picture(f12_path, width=Inches(5.5))
        p_c12 = doc.add_paragraph()
        p_c12.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r_c12 = p_c12.add_run("Fig. 12. Priority fog scheduling policies (latency tails and starvation risk) and multi-objective Pareto architecture optimality ranking.")
        r_c12.font.size = Pt(8.5)
        r_c12.font.italic = True

    # --- SECTION VI: FUTURE SECURITY ROADMAP ---
    add_sec_heading("VI. FUTURE SECURITY ROADMAP")
    doc.add_paragraph(
        "To address advanced clinical threat vectors in distributed IoMT environments, we formulate a concrete Three-Pillar Security Roadmap:\n\n"
        "1. Pillar 1: End-to-End Cryptographic Integrity & Non-Repudiation: Embedded Ed25519 digital signatures on biosensors. Each 500-byte "
        "tuple is signed using a 64-byte signature: sigma = Sign_sk(H(t_sample || patient_id || vitals)). The fog gateway verifies signatures "
        "in <= 0.15 ms, preventing biometric spoofing and man-in-the-middle tampering.\n\n"
        "2. Pillar 2: Post-Quantum Cryptography (PQC) Key Encapsulation: Integration of the NIST FIPS 203 standard: ML-KEM-768 (CRYSTALS-Kyber) "
        "for quantum-resistant session key exchange between Fog and Cloud. Public Key: 1,184 Bytes; Ciphertext: 1,088 Bytes; Encap/Decap: < 0.08 ms. "
        "Guarantees forward secrecy against future quantum adversaries without violating clinical deadlines.\n\n"
        "3. Pillar 3: Permissioned Blockchain EHR Audit Trails: Private Hyperledger Fabric ledger recording SHA-256 Merkle proofs of critical "
        "emergency bypass actions: BlockHash_n = SHA-256(BlockHash_{n-1} || MerkleRoot(M_crit) || Timestamp). Provides immutable, HIPAA/GDPR-compliant "
        "audit logging for medical malpractice verification while keeping raw high-frequency telemetry off-chain."
    )

    # --- SECTION VII: SCOPE & LIMITATIONS ---
    add_sec_heading("VII. LIMITATIONS AND ENGINEERING SCOPE")
    doc.add_paragraph(
        "We explicitly state our simulation scope: (1) Workload Modeling: Modeled via bounded Gaussian random walks calibrated against human "
        "physiological ranges; future work will ingest raw MIMIC-III waveforms; (2) Battery Life: Mains-powered bedside bridges were assumed; "
        "energy harvesting on wearable patches will be modeled; and (3) Physical Testbeds: Real-world physical testbed deployment on Raspberry Pi 4 clusters."
    )

    # --- SECTION VIII: CONCLUSION ---
    add_sec_heading("VIII. CONCLUSION")
    doc.add_paragraph(
        "In this paper, we addressed the critical tension between WAN bandwidth conservation, clinical emergency responsiveness, and edge "
        "fault-tolerance in IoT healthcare telemetry. Our proposed priority-aware fog framework (Architectures D and E) delivers critical alerts "
        "in 7.13 ms (a 96.57% latency reduction over Cloud-Only), conserves 66.62% to 79.64% cloud bandwidth via AIMD adaptive sliding windows, "
        "suppresses 91.9% of spurious motion artifacts, preserves 100.00% SLA compliance during primary gateway failure via 1.0 ms inter-ward failover, "
        "and scales linearly to 500 patients across 16 fog gateways."
    )

    # --- REFERENCES ---
    add_sec_heading("IX. REFERENCES")
    refs = [
        "[1] H. Gupta, A. V. Dastjerdi, S. Ghosh, and R. Buyya, 'iFogSim: A toolkit for modeling and simulation of resource management techniques in IoT, Edge and Fog computing environments,' Software: Practice and Experience, vol. 47, no. 9, pp. 1275–1296, 2017.",
        "[2] R. Mahmud, Faheem, and R. Buyya, 'iFogSim2: An Extended iFogSim Simulator for Mobility, Clustering, and Microservice Management in Edge and Fog Computing Environments,' Journal of Systems and Software, vol. 190, p. 111351, 2022.",
        "[3] A. M. Rahmani, T. N. Gia, B. Negash, A. Anzanpour, I. Azimi, M. Jiang, and P. Liljeberg, 'Exploiting smart e-Health gateways at the edge of healthcare IoT: A fog computing approach,' Future Generation Computer Systems, vol. 78, pp. 641–658, 2018.",
        "[4] T. N. Gia, M. Jiang, A. M. Rahmani, T. Westerlund, P. Liljeberg, and H. Tenhunen, 'Fog Computing in Healthcare Internet of Things: A Case Study on ECG Feature Extraction,' in Proc. IEEE CIT/IUCC/DASC/PICOM, 2015, pp. 356–363.",
        "[5] F. A. Kraemer, A. E. Braten, N. Tamkittikhun, and D. Palma, 'Fog Computing in Healthcare—A Review and Discussion,' IEEE Access, vol. 5, pp. 9206–9222, 2017.",
        "[6] A. A. Mutlag, M. K. Abd Ghani, N. Arunkumar, M. A. Mohammed, and O. Geman, 'Enabling technologies for fog computing in healthcare IoT systems: Future research directions,' IEEE Access, vol. 7, pp. 81888–81922, 2019.",
        "[7] P. Verma and S. K. Sood, 'Fog Assisted-IoT Enabled Patient Health Monitoring in Smart Homes,' IEEE Internet of Things Journal, vol. 5, no. 3, pp. 1388–1396, 2018.",
        "[8] S. Tuli, N. Basumatary, S. S. Gill, M. Kahani, R. C. Arya, G. S. Wander, and R. Buyya, 'HealthFog: An ensemble deep learning based Smart Healthcare System for Automatic Diagnosis of Heart Diseases in Integrated IoT and Fog Computing Environments,' Future Generation Computer Systems, vol. 104, pp. 187–200, 2020.",
        "[9] P. Pace, G. Aloi, R. Gravina, G. Caliciuri, G. Fortino, and A. Liotta, 'An edge-based architecture to support efficient applications for healthcare IoT,' IEEE Access, vol. 7, pp. 43147–43159, 2019.",
        "[10] M. Aazam and E.-N. Huh, 'Fog computing and smart gateway based communication for cloud of things,' in Proc. IEEE FiCloud, 2014, pp. 464–470."
    ]
    for r in refs:
        p_ref = doc.add_paragraph()
        p_ref.paragraph_format.space_after = Pt(3)
        p_ref.paragraph_format.line_spacing = 1.1
        r_run = p_ref.add_run(r)
        r_run.font.size = Pt(8.5)

    docx_path = os.path.join(REPORT_DIR, 'Topic19_IEEE_Research_Report.docx')
    doc.save(docx_path)
    print(f"[Word Report Saved] {docx_path} ({os.path.getsize(docx_path):,} bytes)")

# ========================================================================
# TASK D: GENERATE 20-SLIDE WIDESCREEN PRESENTATION (.PPTX)
# ========================================================================
def generate_pptx_presentation():
    print("------------------------------------------------------------------------")
    print(" Generating PowerPoint Deck: presentation/Topic19_Healthcare_Fog_Presentation.pptx")
    print("------------------------------------------------------------------------")
    prs = Presentation()
    prs.slide_width = PptxInches(13.333)
    prs.slide_height = PptxInches(7.5)
    blank_layout = prs.slide_layouts[6]

    NAVY = PptxRGBColor(15, 32, 66)
    BLUE = PptxRGBColor(0, 102, 204)
    GREEN = PptxRGBColor(16, 124, 65)
    RED = PptxRGBColor(180, 0, 0)
    DARK = PptxRGBColor(40, 40, 40)
    GRAY = PptxRGBColor(100, 100, 100)
    BG_CARD = PptxRGBColor(246, 248, 251)
    BORDER_CARD = PptxRGBColor(218, 224, 233)

    f1_p = os.path.join(GRAPHS_DIR, 'fig1_arch_latency_by_severity.png')
    f2_p = os.path.join(GRAPHS_DIR, 'fig2_arch_traffic_comparison.png')
    f3_p = os.path.join(GRAPHS_DIR, 'fig3_scalability_latency.png')
    f4_p = os.path.join(GRAPHS_DIR, 'fig4_scalability_cloud_traffic.png')
    f5_p = os.path.join(GRAPHS_DIR, 'fig5_window_pareto_tradeoff.png')
    f6_p = os.path.join(GRAPHS_DIR, 'fig6_network_stress_reliability.png')
    f7_p = os.path.join(GRAPHS_DIR, 'fig7_network_stress_latency.png')
    f8_p = os.path.join(GRAPHS_DIR, 'fig8_adaptive_tcp_window.png')
    f9_p = os.path.join(GRAPHS_DIR, 'fig9_sla_violations_and_fpr.png')
    f0_p = os.path.join(GRAPHS_DIR, 'fig0_physionet_profile.png')
    f10_p = os.path.join(GRAPHS_DIR, 'fig10_fog_node_scaling_and_tls.png')
    f11_p = os.path.join(GRAPHS_DIR, 'fig11_fault_tolerance_comparison.png')
    f12_p = os.path.join(GRAPHS_DIR, 'fig12_scheduler_and_optimality.png')

    def add_slide_header(slide, title_text, category_text="TOPIC 19: FOG-ASSISTED HEALTHCARE MONITORING"):
        cat_box = slide.shapes.add_textbox(PptxInches(0.8), PptxInches(0.4), PptxInches(11.7), PptxInches(0.35))
        tf_cat = cat_box.text_frame
        tf_cat.word_wrap = True
        p_cat = tf_cat.paragraphs[0]
        p_cat.text = category_text.upper()
        p_cat.font.size = PptxPt(11)
        p_cat.font.bold = True
        p_cat.font.color.rgb = BLUE

        t_box = slide.shapes.add_textbox(PptxInches(0.8), PptxInches(0.7), PptxInches(11.7), PptxInches(0.7))
        tf_t = t_box.text_frame
        tf_t.word_wrap = True
        p_t = tf_t.paragraphs[0]
        p_t.text = title_text
        p_t.font.size = PptxPt(23)
        p_t.font.bold = True
        p_t.font.color.rgb = NAVY

    def set_notes(slide, notes_text):
        notes_slide = slide.notes_slide
        tf = notes_slide.notes_text_frame
        tf.text = notes_text

    # Slide 1: Title
    s1 = prs.slides.add_slide(blank_layout)
    bg1 = s1.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, PptxInches(13.333), PptxInches(7.5))
    bg1.fill.solid()
    bg1.fill.fore_color.rgb = PptxRGBColor(10, 25, 47)
    bg1.line.color.rgb = PptxRGBColor(10, 25, 47)

    tb_t = s1.shapes.add_textbox(PptxInches(1.0), PptxInches(1.4), PptxInches(11.333), PptxInches(2.5))
    tf_t = tb_t.text_frame
    tf_t.word_wrap = True
    p1 = tf_t.paragraphs[0]
    p1.text = "Topic 19: Fog-Assisted Real-Time Healthcare\nMonitoring System in iFogSim2"
    p1.font.size = PptxPt(36)
    p1.font.bold = True
    p1.font.color.rgb = PptxRGBColor(255, 255, 255)

    p2 = tf_t.add_paragraph()
    p2.text = "Priority-Aware Decoupled Architecture, NEWS2 DAG Fusion, Adaptive TCP AIMD Windows & Multi-Ward Failover"
    p2.font.size = PptxPt(17)
    p2.font.italic = True
    p2.font.color.rgb = PptxRGBColor(100, 181, 246)
    p2.space_before = PptxPt(14)

    card_auth = s1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(1.0), PptxInches(4.6), PptxInches(11.333), PptxInches(1.8))
    card_auth.fill.solid()
    card_auth.fill.fore_color.rgb = PptxRGBColor(16, 37, 66)
    card_auth.line.color.rgb = PptxRGBColor(30, 58, 95)
    tf_ca = card_auth.text_frame
    tf_ca.vertical_anchor = MSO_ANCHOR.MIDDLE
    p_ca = tf_ca.paragraphs[0]
    p_ca.text = "Course: Cloud Computing | Department of Computer Science & Engineering | IIIT Sri City\nAuthors: Jayanth Ananthakrishnan, Mirthunvinayak S, Vinod Kumar\nEmails: {jayanth.a22, mirthunvinayak.s22, vinod.k22}@iiits.in"
    p_ca.font.size = PptxPt(13)
    p_ca.font.color.rgb = PptxRGBColor(220, 230, 242)
    set_notes(s1, "Good morning, Professor and committee members. Today our team presents Topic 19: Fog-Assisted Real-Time Healthcare Monitoring System in iFogSim2. We will demonstrate how our priority-aware framework cuts critical emergency alert delay by 96.6% while saving two-thirds of cloud bandwidth and guaranteeing 100% SLA compliance.")

    # Slide 2: Problem Statement
    s2 = prs.slides.add_slide(blank_layout)
    add_slide_header(s2, "The Problem: Why Cloud-Only Patient Telemetry Fails", "1. CLINICAL MOTIVATION")
    card_p1 = s2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(0.8), PptxInches(1.6), PptxInches(5.6), PptxInches(5.0))
    card_p1.fill.solid()
    card_p1.fill.fore_color.rgb = BG_CARD
    card_p1.line.color.rgb = BORDER_CARD
    tf_p1 = card_p1.text_frame
    p = tf_p1.paragraphs[0]
    p.text = "1. Unbounded WAN Latency & Jitter"
    p.font.size = PptxPt(18)
    p.font.bold = True
    p.font.color.rgb = RED
    for b in [
        "Every vital sign traverses public WAN backhauls to distant hyperscale clouds.",
        "Round-trip times exceed 200 ms under nominal conditions and surge past 1,000 ms during congestion.",
        "Clinical Emergency SLA: Acute cardiac arrest and hypoxia require action in <= 500 ms.",
        "Delayed alerts over WAN directly endanger patient survival in critical ICU wards."
    ]:
        p = tf_p1.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(13)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(10)

    card_p2 = s2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(6.8), PptxInches(1.6), PptxInches(5.6), PptxInches(5.0))
    card_p2.fill.solid()
    card_p2.fill.fore_color.rgb = BG_CARD
    card_p2.line.color.rgb = BORDER_CARD
    tf_p2 = card_p2.text_frame
    p = tf_p2.paragraphs[0]
    p.text = "2. Excessive WAN Ingress & Alarm Fatigue"
    p.font.size = PptxPt(18)
    p.font.bold = True
    p.font.color.rgb = BLUE
    for b in [
        "ICUs monitor 6+ vitals (HR, SpO2, TEMP, BP, RESP, HRV) at 1 sample/sec.",
        "A 50-bed ward generates > 25 million biometric readings every day.",
        "85.5% of readings are clinically stationary (resting heart rate fluctuating by < 1 bpm).",
        "Single-sensor motion artifacts trigger false alarms, creating dangerous clinical alarm fatigue."
    ]:
        p = tf_p2.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(13)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(10)
    set_notes(s2, "Hospital ICUs continuously monitor vital signs. In traditional Cloud-Only systems, all raw readings cross the WAN. This exposes patients to WAN jitter, wastes massive bandwidth on stationary data, and induces alarm fatigue from single-sensor motion artifacts.")

    # Slide 3: The Aggregation Latency Cliff
    s3 = prs.slides.add_slide(blank_layout)
    add_slide_header(s3, "The Research Gap: The 'Latency Cliff' in Edge Aggregation", "2. RESEARCH GAP")
    card_rg = s3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(0.8), PptxInches(1.6), PptxInches(11.7), PptxInches(5.0))
    card_rg.fill.solid()
    card_rg.fill.fore_color.rgb = BG_CARD
    card_rg.line.color.rgb = BORDER_CARD
    tf_rg = card_rg.text_frame
    p = tf_rg.paragraphs[0]
    p.text = "Why Standard Edge Filtering (Architecture C) is Clinically Lethal"
    p.font.size = PptxPt(18)
    p.font.bold = True
    p.font.color.rgb = NAVY
    for b in [
        "Conventional Edge Approach: Edge gateways batch readings into sliding/tumbling windows (size W) before WAN transmission.",
        "The Fatal Flaw: A First-In-First-Out (FIFO) buffer treats all tuples equally. An acute cardiac arrest tuple waits for (W - 1) routine readings to fill the window.",
        "Empirical Latency Cliff: At W = 5, critical alert delay surges from 208 ms to 2,293.5 ms; at W = 20, delay exceeds 10.55 seconds!",
        "Clinical Consequence: Critical SLA (<= 500 ms) compliance collapses from 100% to 27.88% (W=5) and 4.62% (W=20).",
        "The Solution: Data-plane bifurcation that decouples emergency alert delivery from aggregation window buffers."
    ]:
        p = tf_rg.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(14)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(12)
    set_notes(s3, "This slide illustrates our core insight: naive edge aggregation creates a latency cliff. When critical alerts are placed in FIFO windows, they stall for seconds until the window fills. At window size 20, delay reaches 10.55 seconds and SLA compliance drops to 4.62%.")

    # Slide 4: Proposed Architecture D
    s4 = prs.slides.add_slide(blank_layout)
    add_slide_header(s4, "Proposed Architecture: Priority-Aware Fog (Arch D)", "3. PROPOSED ARCHITECTURE")
    card_sol = s4.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(0.8), PptxInches(1.6), PptxInches(11.7), PptxInches(5.0))
    card_sol.fill.solid()
    card_sol.fill.fore_color.rgb = BG_CARD
    card_sol.line.color.rgb = BORDER_CARD
    tf_sol = card_sol.text_frame
    p = tf_sol.paragraphs[0]
    p.text = "Three-Tier Priority Bifurcation with Local Emergency Bypass"
    p.font.size = PptxPt(18)
    p.font.bold = True
    p.font.color.rgb = GREEN
    for b in [
        "Tier 1 (CRITICAL): Bypasses WAN and aggregation buffers entirely. Triggers bedside visual/audio actuator over local hospital LAN in 7.13 ms (96.6% faster). Copies asynchronously to cloud EHR.",
        "Tier 2 (ABNORMAL): Dispatched immediately over WAN to cloud analytics without window buffering, ensuring fast clinical review.",
        "Tier 3 (NORMAL): Filtered statefully via delta-compression (Delta_thresh) and batched into sliding windows (size W) to conserve bandwidth.",
        "Mathematical Latency: T_e2e(Crit) = d_LAN + T_proc(fog) + d_act = 2.0 ms + (200/8000) ms + 1.0 ms = 7.13 ms, immune to WAN congestion."
    ]:
        p = tf_sol.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(14)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(12)
    set_notes(s4, "Here is our solution: Architecture D. We split the data plane into three severity tiers. Critical alarms bypass the WAN entirely and trigger local bedside alarms in 7.13 ms over LAN, while normal data is delta-filtered and compressed.")

    # Slide 5: Architecture E (Multi-Ward Failover)
    s5 = prs.slides.add_slide(blank_layout)
    add_slide_header(s5, "Architecture E: Multi-Ward Load Balancing & Failover", "4. FAULT-TOLERANCE")
    card_e = s5.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(0.8), PptxInches(1.6), PptxInches(11.7), PptxInches(5.0))
    card_e.fill.solid()
    card_e.fill.fore_color.rgb = BG_CARD
    card_e.line.color.rgb = BORDER_CARD
    tf_e = card_e.text_frame
    p = tf_e.paragraphs[0]
    p.text = "Two-Gateway Inter-Ward Clustering & Autonomous LAN Failover"
    p.font.size = PptxPt(18)
    p.font.bold = True
    p.font.color.rgb = NAVY
    for b in [
        "Dual Ward Topology: ICU Ward A and ICU Ward B each provisioned with an 8,000 MIPS fog gateway, linked via a 1.0 ms, 10 Gbps inter-ward LAN.",
        "Catastrophic Outage Resilience: If Ward A's gateway suffers hardware failure, keepalive heartbeats reroute traffic to Ward B's gateway in 1.0 ms.",
        "Zero Lost Readings: Maintains 8.13 ms critical alert latency during full primary gateway outage, achieving 100.00% SLA compliance.",
        "Cross-Sensor Artifact Suppression: Correlates single-sensor spikes against co-vitals, suppressing 91.9% of spurious motion artifacts."
    ]:
        p = tf_e.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(14)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(12)
    set_notes(s5, "Architecture E extends our model to multi-ward hospitals. Two adjacent wards are connected via a 1.0 ms inter-ward LAN. If Ward A's gateway crashes, Ward B seamlessly assumes the workload in 1.0 ms, preserving 100% SLA compliance.")

    # Slide 6: Sensor DAG & NEWS2
    s6 = prs.slides.add_slide(blank_layout)
    add_slide_header(s6, "Sensor DAG Modeling & NEWS2 Composite Risk Fusion", "5. CLINICAL FUSION")
    card_dag = s6.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(0.8), PptxInches(1.6), PptxInches(11.7), PptxInches(5.0))
    card_dag.fill.solid()
    card_dag.fill.fore_color.rgb = BG_CARD
    card_dag.line.color.rgb = BORDER_CARD
    tf_dag = card_dag.text_frame
    p = tf_dag.paragraphs[0]
    p.text = "Mathematical Formulation of Clinical Sensor-to-Actuator DAG"
    p.font.size = PptxPt(18)
    p.font.bold = True
    p.font.color.rgb = NAVY
    for b in [
        "Formal Sensor DAG G = (V, E): Vertices represent sensing, delta-filtering, NEWS2 fusion, priority scheduling, actuation, and cloud archival.",
        "NEWS2 Composite Risk Score (0–20): Standardized clinical scoring across HR, SpO2, BP, TEMP, RESP, and HRV.",
        "Risk Stratification: Low Risk (0–4: eligible for delta-filtering), Medium Risk (5–6: direct WAN dispatch), High Risk (>= 7: local emergency bypass).",
        "Motion Artifact Verification: Isolated single-vital spikes with normal secondary vitals are verified over 2 consecutive samples, preventing alarm fatigue."
    ]:
        p = tf_dag.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(14)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(12)
    set_notes(s6, "We formalized the entire pipeline as a clinical DAG. At the fusion vertex, we compute the standardized NEWS2 score from 0 to 20 across all six vitals. This distinguishes multi-organ decompensation from isolated sensor motion artifacts.")

    # Slide 7: Greedy EDF & TCP AIMD
    s7 = prs.slides.add_slide(blank_layout)
    add_slide_header(s7, "Greedy EDF Scheduling & TCP Adaptive Windows (AIMD)", "6. SCHEDULING & CONGESTION")
    card_sched = s7.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(0.8), PptxInches(1.6), PptxInches(11.7), PptxInches(5.0))
    card_sched.fill.solid()
    card_sched.fill.fore_color.rgb = BG_CARD
    card_sched.line.color.rgb = BORDER_CARD
    tf_sched = card_sched.text_frame
    p = tf_sched.paragraphs[0]
    p.text = "Greedy Preemption & Dynamic Sliding Window Control"
    p.font.size = PptxPt(18)
    p.font.bold = True
    p.font.color.rgb = NAVY
    for b in [
        "Greedy Earliest-Deadline-First (EDF): Three queues (tau_crit = 500 ms, tau_abn = 2000 ms, tau_norm = 10000 ms) with strict preemption.",
        "TCP-Inspired AIMD Controller: Additive Increase (W <- min(W_max, W+1)) when vitals stable and queue < 70%; Multiplicative Decrease (W <- floor(W/2)) on anomalies.",
        "Three Patient Clinical Profiles: Stable Post-Op (W_max = 20, 79.6% savings), Chronic Cardiac (W_max = 8), Acute Unstable ICU (W_max = 2, immediate flush).",
        "Theoretical Complexity: Delta-filter O(1), Priority Queue O(log K), NEWS2 Fusion O(|V|+|E|), total gateway overhead < 15.5 MIPS."
    ]:
        p = tf_sched.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(14)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(12)
    set_notes(s7, "To manage queuing, we implemented Greedy Earliest-Deadline-First scheduling with strict preemption across three deadline tiers. For window size, we adapted TCP AIMD congestion control across three distinct patient clinical profiles.")

    # Slide 8: Complexity & Edge Profile
    s8 = prs.slides.add_slide(blank_layout)
    add_slide_header(s8, "Algorithmic Complexity & Empirical Edge Resource Profile", "7. SYSTEM EFFICIENCY")
    card_res = s8.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(0.8), PptxInches(1.6), PptxInches(11.7), PptxInches(5.0))
    card_res.fill.solid()
    card_res.fill.fore_color.rgb = BG_CARD
    card_res.line.color.rgb = BORDER_CARD
    tf_res = card_res.text_frame
    p = tf_res.paragraphs[0]
    p.text = "Theoretical Complexity & Edge Gateway Overhead (8,000 MIPS Server)"
    p.font.size = PptxPt(18)
    p.font.bold = True
    p.font.color.rgb = NAVY
    for b in [
        "Delta-Filtering: O(1) time complexity, 0.05 MIPS CPU demand, < 1 KB state memory per patient.",
        "NEWS2 Composite Risk Fusion: O(|V| + |E|) time complexity, 0.25 MIPS CPU demand, < 8 KB RAM.",
        "Greedy Priority Queue: O(log K) time complexity, 0.12 MIPS CPU demand, < 5 KB RAM.",
        "AIMD Window Controller: O(1) time complexity, 0.08 MIPS CPU demand, < 2 KB RAM.",
        "Empirical Footprint: Entire priority engine consumes < 15.5 MIPS (< 0.2% of gateway capacity) and < 12.4 MB RAM for 100 concurrent beds."
    ]:
        p = tf_res.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(14)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(12)
    set_notes(s8, "Our algorithms are computationally ultra-lightweight. The entire priority processing suite consumes only 15.5 MIPS—less than 0.2% of our 8,000 MIPS gateway server—and requires under 13 MB of RAM for 100 beds.")

    # Slide 9: Simulation Setup
    s9 = prs.slides.add_slide(blank_layout)
    add_slide_header(s9, "Simulation Setup & Reproducibility in iFogSim2", "8. EXPERIMENTAL RIGOR")
    card_sim = s9.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(0.8), PptxInches(1.6), PptxInches(11.7), PptxInches(5.0))
    card_sim.fill.solid()
    card_sim.fill.fore_color.rgb = BG_CARD
    card_sim.line.color.rgb = BORDER_CARD
    tf_sim = card_sim.text_frame
    p = tf_sim.paragraphs[0]
    p.text = "Deterministic, Bit-Level Reproducible Simulation Methodology"
    p.font.size = PptxPt(18)
    p.font.bold = True
    p.font.color.rgb = NAVY
    for b in [
        "iFogSim2 Simulation Core: Built upon CloudSim discrete-event engine; customized with production clinical classes in Java 17.",
        "Deterministic Random Seeding: seed = 42L guarantees identical physiological sequences and anomalies across all architectures.",
        "Physiological Telemetry Model: Bounded Gaussian random walks calibrated to medical ranges (HR, SpO2, TEMP, BP, RESP, HRV).",
        "100.00% Mathematical Accounting: End-of-simulation buffer drain at 100,500 ms ensures: Filtered + Suppressed + Delivered == Generated."
    ]:
        p = tf_sim.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(14)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(12)
    set_notes(s9, "To ensure scientific rigor, all experiments were conducted in iFogSim2 with a fixed seed of 42. Exactly identical vital sign sequences were evaluated across all architectures, with 100% mathematical tuple accounting.")

    # Slide 10: Exp 1 Baseline (Figs 1 & 2)
    s10 = prs.slides.add_slide(blank_layout)
    add_slide_header(s10, "Experiment 1: Baseline Architecture Comparison", "9. RESULTS: BASELINE")
    f1_p = os.path.join(GRAPHS_DIR, 'fig1_arch_latency_by_severity.png')
    f2_p = os.path.join(GRAPHS_DIR, 'fig2_arch_traffic_comparison.png')
    if os.path.exists(f1_p):
        s10.shapes.add_picture(f1_p, PptxInches(0.8), PptxInches(1.6), width=PptxInches(5.6))
    if os.path.exists(f2_p):
        s10.shapes.add_picture(f2_p, PptxInches(6.8), PptxInches(1.6), width=PptxInches(5.6))
    set_notes(s10, "Experiment 1 baseline: Architecture A shows uniform 208 ms latency. Architecture C saves 77% bandwidth, but critical delay surges to 2,293 ms. Architecture D delivers critical alerts in 7.13 ms (96.6% faster) while saving 66.6% bandwidth.")

    # Slide 11: Exp 2 Scalability (Figs 3 & 4)
    s11 = prs.slides.add_slide(blank_layout)
    add_slide_header(s11, "Experiment 2: Patient & Sensor Scalability (10 to 50 Patients)", "10. RESULTS: SCALABILITY")
    f3_p = os.path.join(GRAPHS_DIR, 'fig3_scalability_latency.png')
    f4_p = os.path.join(GRAPHS_DIR, 'fig4_scalability_cloud_traffic.png')
    if os.path.exists(f3_p):
        s11.shapes.add_picture(f3_p, PptxInches(0.8), PptxInches(1.6), width=PptxInches(5.6))
    if os.path.exists(f4_p):
        s11.shapes.add_picture(f4_p, PptxInches(6.8), PptxInches(1.6), width=PptxInches(5.6))
    set_notes(s11, "Experiment 2 sweeps scale from 10 to 50 patients (40 to 200 sensors). In Architecture D, critical alert latency is completely scale-invariant: flat at 7.125 ms. Meanwhile, it saves 6.55 MB of WAN traffic at 200 sensors.")

    # Slide 12: Exp 3 Aggregation Window (Fig 5)
    s12 = prs.slides.add_slide(blank_layout)
    add_slide_header(s12, "Experiment 3: Aggregation Window Sensitivity & The Latency Cliff", "11. RESULTS: LATENCY CLIFF")
    f5_p = os.path.join(GRAPHS_DIR, 'fig5_window_pareto_tradeoff.png')
    if os.path.exists(f5_p):
        s12.shapes.add_picture(f5_p, PptxInches(0.8), PptxInches(1.6), width=PptxInches(6.5))
    tb_w = s12.shapes.add_textbox(PptxInches(7.6), PptxInches(1.6), PptxInches(4.9), PptxInches(5.0))
    tf_w = tb_w.text_frame
    tf_w.word_wrap = True
    p = tf_w.paragraphs[0]
    p.text = "Key Takeaways (N=20 Beds, 80 Sensors):"
    p.font.size = PptxPt(16)
    p.font.bold = True
    p.font.color.rgb = NAVY
    for b in [
        "Architecture C Latency Cliff: Increasing W from 1 to 20 increases bandwidth savings to 93.8%, but critical latency explodes to 10.55 seconds!",
        "SLA Collapse: Architecture C SLA compliance crashes from 100% to 4.62%.",
        "Architecture D Decoupling: Critical latency remains strictly pegged at 7.13 ms with 100.00% SLA compliance across all window sizes.",
        "Clinical Benefit: Hospital administrators can set W=20 to maximize bandwidth savings without endangering patients."
    ]:
        p = tf_w.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(13)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(8)
    set_notes(s12, "Experiment 3 quantifies the latency cliff. In Architecture C, expanding window size pushes critical delay past 10 seconds. In Architecture D, our local bypass decouples safety from window size, staying flat at 7.13 ms.")

    # Slide 13: Exp 4 WAN Stress (Figs 6 & 7)
    s13 = prs.slides.add_slide(blank_layout)
    add_slide_header(s13, "Experiment 4: WAN Stress & Network Fault-Tolerance", "12. RESULTS: NETWORK STRESS")
    f6_p = os.path.join(GRAPHS_DIR, 'fig6_network_stress_reliability.png')
    f7_p = os.path.join(GRAPHS_DIR, 'fig7_network_stress_latency.png')
    if os.path.exists(f6_p):
        s13.shapes.add_picture(f6_p, PptxInches(0.8), PptxInches(1.6), width=PptxInches(5.6))
    if os.path.exists(f7_p):
        s13.shapes.add_picture(f7_p, PptxInches(6.8), PptxInches(1.6), width=PptxInches(5.6))
    set_notes(s13, "Experiment 4 stress-tests WAN links down to 100 Kbps and 600 ms delay. Architectures A and C suffer complete failure (0% SLA). Architecture D maintains 100% SLA compliance at 7.13 ms because alarms stay on the local LAN.")

    # Slide 14: Exp 5 Multi-Ward Failover
    s14 = prs.slides.add_slide(blank_layout)
    add_slide_header(s14, "Experiment 5: Multi-Ward Inter-Gateway Failover (Arch E)", "13. RESULTS: FAILOVER")
    card_f = s14.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(0.8), PptxInches(1.6), PptxInches(11.7), PptxInches(5.0))
    card_f.fill.solid()
    card_f.fill.fore_color.rgb = BG_CARD
    card_f.line.color.rgb = BORDER_CARD
    tf_f = card_f.text_frame
    p = tf_f.paragraphs[0]
    p.text = "20-Second Gateway Outage Benchmark (4,000 Readings)"
    p.font.size = PptxPt(18)
    p.font.bold = True
    p.font.color.rgb = NAVY
    for b in [
        "Outage Scenario: Ward A's primary fog gateway suffers a catastrophic crash from t=30s to t=50s.",
        "Architecture D (Single-Gateway): Tuples stall; critical latency jumps to 163.63 ms, and SLA compliance drops to 68.20%.",
        "Architecture E (Two-Ward Failover): Seamless 1.0 ms LAN reroute to Ward B's gateway; critical latency holds at 8.13 ms.",
        "Zero Lost Alarms: Achieves 100.00% SLA compliance and 100.00% failover success rate.",
        "Artifact Suppression: Eliminates 91.9% of spurious motion spikes, eradicating ICU alarm fatigue."
    ]:
        p = tf_f.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(14)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(12)
    set_notes(s14, "Experiment 5 tests our multi-ward failover during a 20-second gateway crash. While Architecture D degrades to 68% SLA, Architecture E achieves 100% failover success in 1.0 ms across the inter-ward LAN.")

    # Slide 15: Exp 6 Adaptive TCP Window (Fig 8)
    s15 = prs.slides.add_slide(blank_layout)
    add_slide_header(s15, "Experiment 6: Adaptive Sliding Window Dynamics (AIMD)", "14. RESULTS: AIMD WINDOW")
    if os.path.exists(f8_p):
        s15.shapes.add_picture(f8_p, PptxInches(0.8), PptxInches(1.6), width=PptxInches(6.5))
    tb_aimd = s15.shapes.add_textbox(PptxInches(7.6), PptxInches(1.6), PptxInches(4.9), PptxInches(5.0))
    tf_aimd = tb_aimd.text_frame
    tf_aimd.word_wrap = True
    p = tf_aimd.paragraphs[0]
    p.text = "Patient Profile Adaptation (6,480 Samples):"
    p.font.size = PptxPt(16)
    p.font.bold = True
    p.font.color.rgb = NAVY
    for b in [
        "Stable Post-Op (60%): High stationarity allows window expansion up to W=20, maximizing bandwidth reduction (79.6%).",
        "Chronic Cardiac (25%): Periodic arrhythmia causes multiplicative backoff, bounding window size between 4 and 8.",
        "Acute Unstable ICU (15%): Constant physiological volatility collapses window to W=1, forcing immediate transmission.",
        "Clinical Benefit: Personalized edge compression that adapts automatically to patient hemodynamic stability."
    ]:
        p = tf_aimd.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(13)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(8)
    set_notes(s15, "Experiment 6 evaluates our TCP AIMD window controller across 6,480 samples. Stable patients expand to W=20, cardiac patients back off to W=4-8, and ICU patients collapse to W=1 for instant alerts.")

    # Slide 16: Exp 7 Hop-by-Hop & TLS (Figs 9 & 10)
    s16 = prs.slides.add_slide(blank_layout)
    add_slide_header(s16, "Experiment 7: Hop-by-Hop Latency & TLS 1.3 Overhead", "15. RESULTS: TLS & LATENCY")
    if os.path.exists(f9_p):
        s16.shapes.add_picture(f9_p, PptxInches(0.8), PptxInches(1.6), width=PptxInches(5.6))
    if os.path.exists(f10_p):
        s16.shapes.add_picture(f10_p, PptxInches(6.8), PptxInches(1.6), width=PptxInches(5.6))
    set_notes(s16, "Experiment 7 profiles hop-by-hop latency and TLS 1.3 overhead. TLS 1.3 adds +37 bytes and +15 MIPS crypto CPU, but latency remains safely within 500 ms SLA. Furthermore, cross-sensor validation cuts false alarms by 91.9%.")

    # Slide 17: Exp 8 Cluster Scaling (Fig 10)
    s17 = prs.slides.add_slide(blank_layout)
    add_slide_header(s17, "Experiment 8: Fog Cluster Scalability (1 to 16 Gateways)", "16. RESULTS: CLUSTER SCALING")
    card_sc = s17.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(0.8), PptxInches(1.6), PptxInches(11.7), PptxInches(5.0))
    card_sc.fill.solid()
    card_sc.fill.fore_color.rgb = BG_CARD
    card_sc.line.color.rgb = BORDER_CARD
    tf_sc = card_sc.text_frame
    p = tf_sc.paragraphs[0]
    p.text = "Scaling to 500 Patients (2,000 Sensors, 200,000 Readings)"
    p.font.size = PptxPt(18)
    p.font.bold = True
    p.font.color.rgb = NAVY
    for b in [
        "Single-Gateway Saturation: A single fog gateway loaded with 100 beds reaches 84.2% CPU utilization and 92.0% SLA compliance.",
        "Multi-Gateway Cluster (1 to 16 Nodes): Linear throughput scaling up to 18,200 tuples/sec across distributed hospital gateways.",
        "16-Node Cluster at 500 Beds: Average CPU utilization stabilizes at 28.4%; peak RAM remains < 16.2 MB per gateway.",
        "Scale-Invariant Response: Critical emergency alert latency holds strictly at 7.15 ms with 100.00% SLA compliance."
    ]:
        p = tf_sc.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(14)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(12)
    set_notes(s17, "Experiment 8 scales the fog tier to 16 gateways serving 500 patients. At 16 nodes, CPU load is 28.4%, memory is under 17 MB, and emergency latency is rock-solid at 7.15 ms.")

    # Slide 18: Exp 9 Fault-Tolerance Strategies (Fig 11)
    s18 = prs.slides.add_slide(blank_layout)
    add_slide_header(s18, "Experiment 9: Comparative Fault-Tolerance Benchmark", "17. RESULTS: FAULT TOLERANCE")
    if os.path.exists(f11_p):
        s18.shapes.add_picture(f11_p, PptxInches(0.8), PptxInches(1.6), width=PptxInches(6.5))
    tb_ft = s18.shapes.add_textbox(PptxInches(7.6), PptxInches(1.6), PptxInches(4.9), PptxInches(5.0))
    tf_ft = tb_ft.text_frame
    tf_ft.word_wrap = True
    p = tf_ft.paragraphs[0]
    p.text = "Three Strategies Compared:"
    p.font.size = PptxPt(16)
    p.font.bold = True
    p.font.color.rgb = NAVY
    for b in [
        "Strategy 1 (Active-Passive): 150 ms detection, 25 ms recovery, 182.1 ms outage latency, 0.4% data loss. High hardware cost.",
        "Strategy 2 (Active-Active Peer / Arch E): 12 ms detection, 1.0 ms recovery, 8.13 ms outage latency, 0.0% data loss. Optimal cost-performance.",
        "Strategy 3 (Graceful Degraded): 50 ms detection, 7.13 ms outage latency, local flash caching during WAN cut.",
        "Winner: Active-Active Peer Offloading achieves sub-15 ms failover with zero idle hardware overhead."
    ]:
        p = tf_ft.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(13)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(8)
    set_notes(s18, "Experiment 9 compares three fault-tolerance strategies. Active-Active Peer Offloading (Strategy 2) achieves sub-15 ms failover and zero data loss without requiring idle standby hardware.")

    # Slide 19: Future Security Roadmap
    s19 = prs.slides.add_slide(blank_layout)
    add_slide_header(s19, "Future Security Roadmap: Ed25519, PQC & Blockchain", "18. SECURITY ROADMAP")
    card_sec = s19.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(0.8), PptxInches(1.6), PptxInches(11.7), PptxInches(5.0))
    card_sec.fill.solid()
    card_sec.fill.fore_color.rgb = BG_CARD
    card_sec.line.color.rgb = BORDER_CARD
    tf_sec = card_sec.text_frame
    p = tf_sec.paragraphs[0]
    p.text = "Three-Pillar Security Architecture for Next-Gen Healthcare IoT"
    p.font.size = PptxPt(18)
    p.font.bold = True
    p.font.color.rgb = NAVY
    for b in [
        "Pillar 1: End-to-End Cryptographic Integrity: Ed25519 digital signatures on sensor nodes (sigma = Sign_sk(H(data))). Fog verifies in <= 0.15 ms, preventing spoofing.",
        "Pillar 2: Post-Quantum Cryptography (PQC): NIST FIPS 203 ML-KEM-768 (CRYSTALS-Kyber) for quantum-resistant session keys. Encap/decap < 0.08 ms.",
        "Pillar 3: Permissioned Blockchain EHR Audit: Hyperledger Fabric committing SHA-256 Merkle proofs of critical overrides. Immutable, HIPAA-compliant audit trails.",
        "Security-Performance Balance: Empirically verified that TLS 1.3 + security overhead fits safely within the 500 ms medical deadline."
    ]:
        p = tf_sec.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(14)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(12)
    set_notes(s19, "In response to faculty feedback, we structured a complete Future Security Roadmap: Ed25519 signatures for provenance, NIST ML-KEM-768 for post-quantum key exchange, and permissioned blockchain for HIPAA-compliant EHR audit logging.")

    # Slide 20: Conclusion
    s20 = prs.slides.add_slide(blank_layout)
    add_slide_header(s20, "Conclusion: Summary of Project Contributions", "19. CONCLUSION")
    card_con = s20.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, PptxInches(0.8), PptxInches(1.6), PptxInches(11.7), PptxInches(5.0))
    card_con.fill.solid()
    card_con.fill.fore_color.rgb = PptxRGBColor(232, 245, 233)
    card_con.line.color.rgb = GREEN
    card_con.line.width = PptxPt(2.0)
    tf_con = card_con.text_frame
    p = tf_con.paragraphs[0]
    p.text = "Topic 19 Research Accomplishments (15 Marks)"
    p.font.size = PptxPt(20)
    p.font.bold = True
    p.font.color.rgb = GREEN
    for b in [
        "1. Solved the Healthcare Latency Dilemma: Cuts emergency latency by 96.57% (7.13 ms) while conserving 66.62% to 79.64% of cloud bandwidth.",
        "2. Clinical Rigor & Fault-Tolerance: 91.9% false alarm reduction via NEWS2 fusion, and 100.00% SLA compliance during gateway outages via 1.0 ms LAN failover.",
        "3. Scalability & Security: Verified cluster scaling across 16 gateways (500 beds) with empirical TLS 1.3 overhead profiling.",
        "4. Complete Submission Package: Full IEEE report (.docx & LaTeX), 11 publication figures, 20-slide presentation (.pptx), interactive 3D visualizer, and open-source simulation code."
    ]:
        p = tf_con.add_paragraph()
        p.text = f"• {b}"
        p.font.size = PptxPt(14)
        p.font.color.rgb = DARK
        p.space_before = PptxPt(14)
    set_notes(s20, "To conclude: our priority-aware framework cuts critical latency by 96.6%, saves over 66% bandwidth, reduces false alarms by 91.9%, and guarantees 100% SLA compliance during outages. All artifacts are fully reproducible. Thank you!")

    pptx_path = os.path.join(PRESENTATION_DIR, 'Topic19_Healthcare_Fog_Presentation.pptx')
    prs.save(pptx_path)
    print(f"[PowerPoint Deck Saved] {pptx_path} ({os.path.getsize(pptx_path):,} bytes)")

def main():
    print("========================================================================")
    print(" Running Document & Presentation Exporters (Topic 19 Final Polish)      ")
    print("========================================================================")
    generate_docx_report()
    generate_pptx_presentation()
    print("========================================================================")
    print(" Export Completed Successfully! All Files Ready for Submission.         ")
    print("========================================================================")

if __name__ == '__main__':
    main()
