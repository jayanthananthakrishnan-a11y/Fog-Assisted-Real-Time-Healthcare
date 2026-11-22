#!/usr/bin/env python3
"""
Topic 19: Fog-Assisted Real-Time Healthcare Monitoring System (iFogSim)
PHASE 5: Statistical Analysis & Publication-Quality IEEE Graph Generation
"""

import os
import sys
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import seaborn as sns

# Set high-resolution rendering and publication styling
plt.rcParams['font.family'] = 'sans-serif'
plt.rcParams['font.sans-serif'] = ['DejaVu Sans', 'Arial', 'Helvetica']
plt.rcParams['axes.edgecolor'] = '#333333'
plt.rcParams['axes.linewidth'] = 0.8
plt.rcParams['grid.color'] = '#cccccc'
plt.rcParams['grid.linestyle'] = '--'
plt.rcParams['grid.linewidth'] = 0.5
plt.rcParams['figure.autolayout'] = False

# Colorblind-safe academic palette
PALETTE = {
    'ArchA': '#2b5c8f', # Navy blue
    'ArchB': '#4682b4', # Steel blue
    'ArchC': '#d95f02', # Vermilion / Orange
    'ArchD': '#1b9e77', # Teal / Emerald green
    'Normal': '#7570b3', # Purple
    'Abnormal': '#e6ab02', # Gold
    'Critical': '#e41a1c', # Crimson red
    'SLA': '#d90429',      # Bold alert red
    'Cloud': '#3182bd',    # Blue
    'Total': '#636363'     # Slate grey
}

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
CSV_DIR = os.path.join(BASE_DIR, 'results', 'csv')
GRAPHS_DIR = os.path.join(BASE_DIR, 'results', 'graphs')
PROCESSED_DIR = os.path.join(BASE_DIR, 'results', 'processed')

os.makedirs(GRAPHS_DIR, exist_ok=True)
os.makedirs(PROCESSED_DIR, exist_ok=True)

def load_data():
    exp1_path = os.path.join(CSV_DIR, 'exp1_architecture_comparison.csv')
    exp2_path = os.path.join(CSV_DIR, 'exp2_scalability.csv')
    exp3_path = os.path.join(CSV_DIR, 'exp3_aggregation_window.csv')
    exp4_path = os.path.join(CSV_DIR, 'exp4_network_stress.csv')

    df1 = pd.read_csv(exp1_path)
    df2 = pd.read_csv(exp2_path)
    df3 = pd.read_csv(exp3_path)
    df4 = pd.read_csv(exp4_path)

    # Standardize short architecture names for plotting labels
    for df in [df1, df2, df3, df4]:
        df['arch_short'] = df['architecture'].apply(lambda x: 
            'Arch A (Cloud-Only)' if 'Architecture A' in x else (
            'Arch B (Fog Forward)' if 'Architecture B' in x else (
            'Arch C (Filter & Agg)' if 'Architecture C' in x else (
            'Arch D (Priority Fog)' if 'Architecture D' in x else x))))
        df['arch_code'] = df['architecture'].apply(lambda x:
            'Arch A' if 'Architecture A' in x else (
            'Arch B' if 'Architecture B' in x else (
            'Arch C' if 'Architecture C' in x else (
            'Arch D' if 'Architecture D' in x else x))))
        df['arch_plot_label'] = df['architecture'].apply(lambda x:
            'Arch A\n(Cloud-Only)' if 'Architecture A' in x else (
            'Arch B\n(Fog Forward)' if 'Architecture B' in x else (
            'Arch C\n(Filter & Agg)' if 'Architecture C' in x else (
            'Arch D\n(Priority Fog)' if 'Architecture D' in x else x))))

    return df1, df2, df3, df4

# ========================================================================
# FIGURE 1: Latency by Severity Class across Architectures (Log Scale)
# ========================================================================
def plot_fig1_latency_by_severity(df1):
    fig, ax = plt.subplots(figsize=(7.2, 4.4), dpi=300)
    
    x = np.arange(len(df1))
    width = 0.25

    rects1 = ax.bar(x - width, df1['normal_latency_ms'], width, label='Normal Vitals', 
                    color=PALETTE['Normal'], edgecolor='black', linewidth=0.6, alpha=0.9)
    rects2 = ax.bar(x, df1['abnormal_latency_ms'], width, label='Abnormal Vitals', 
                    color=PALETTE['Abnormal'], edgecolor='black', linewidth=0.6, alpha=0.9)
    rects3 = ax.bar(x + width, df1['critical_latency_ms'], width, label='Critical Vitals', 
                    color=PALETTE['Critical'], edgecolor='black', linewidth=0.6, alpha=0.9)

    ax.set_yscale('log')
    ax.set_ylabel('End-to-End Latency (ms) [Log Scale]', fontsize=11, fontweight='bold')
    ax.set_title('Figure 1: End-to-End Latency by Physiological Severity ($N=10, W=5$)', 
                 fontsize=11.5, fontweight='bold', pad=12)
    ax.set_xticks(x)
    ax.set_xticklabels(df1['arch_plot_label'], fontsize=9.5, fontweight='semibold')
    ax.legend(frameon=True, facecolor='white', edgecolor='#cccccc', fontsize=9.5, loc='upper left')
    ax.grid(True, which='both', axis='y', alpha=0.3)
    ax.set_ylim(2, 20000)

    # Format y ticks with readable numbers
    ax.yaxis.set_major_formatter(ticker.FuncFormatter(lambda y, _: f'{int(y):,}' if y >= 1 else f'{y}'))

    # Annotate bar values
    for rect in rects1:
        h = rect.get_height()
        ax.annotate(f'{h:.1f}', xy=(rect.get_x() + rect.get_width()/2, h),
                    xytext=(0, 3), textcoords="offset points", ha='center', va='bottom', fontsize=7.5, rotation=0)
    for rect in rects2:
        h = rect.get_height()
        ax.annotate(f'{h:.1f}', xy=(rect.get_x() + rect.get_width()/2, h),
                    xytext=(0, 3), textcoords="offset points", ha='center', va='bottom', fontsize=7.5, rotation=0)
    for rect in rects3:
        h = rect.get_height()
        weight = 'bold' if h < 10 else 'normal'
        ax.annotate(f'{h:.2f} ms' if h < 10 else f'{h:.1f}', xy=(rect.get_x() + rect.get_width()/2, h),
                    xytext=(0, 3), textcoords="offset points", ha='center', va='bottom', 
                    fontsize=8, fontweight=weight, color='darkred' if h < 10 else 'black')

    # Add callout arrow pointing to Arch D Critical Bar
    crit_x = x[3] + width
    crit_y = df1.loc[df1['arch_code'] == 'Arch D', 'critical_latency_ms'].values[0]
    ax.annotate('96.6% Faster Emergency Bypass\n(7.13 ms vs 208.0 ms)', 
                xy=(crit_x, crit_y), xytext=(crit_x - 0.7, 25),
                arrowprops=dict(facecolor='darkred', arrowstyle='->', lw=1.2),
                fontsize=8, fontweight='bold', color='darkred',
                bbox=dict(boxstyle="round,pad=0.3", fc="#ffefef", ec="red", lw=0.8))

    plt.tight_layout()
    png_path = os.path.join(GRAPHS_DIR, 'fig1_arch_latency_by_severity.png')
    pdf_path = os.path.join(GRAPHS_DIR, 'fig1_arch_latency_by_severity.pdf')
    plt.savefig(png_path, dpi=300, bbox_inches='tight')
    plt.savefig(pdf_path, bbox_inches='tight')
    plt.close()
    print(f"[Fig 1 Saved] {png_path}")

# ========================================================================
# FIGURE 2: Network Traffic & Bandwidth Savings across Architectures
# ========================================================================
def plot_fig2_network_traffic(df1):
    fig, ax = plt.subplots(figsize=(7.2, 4.4), dpi=300)
    
    x = np.arange(len(df1))
    width = 0.35

    rects1 = ax.bar(x - width/2, df1['cloud_traffic_kb'], width, label='Cloud-Bound WAN Traffic (KB)',
                    color=PALETTE['Cloud'], edgecolor='black', linewidth=0.6, alpha=0.9)
    rects2 = ax.bar(x + width/2, df1['total_traffic_kb'], width, label='Total Network Traffic (KB)',
                    color=PALETTE['Total'], edgecolor='black', linewidth=0.6, alpha=0.7)

    ax.set_ylabel('Network Traffic Volume (KB)', fontsize=11, fontweight='bold')
    ax.set_title('Figure 2: Network Bandwidth Consumption & Edge Savings ($N=10, W=5$)',
                 fontsize=11.5, fontweight='bold', pad=12)
    ax.set_xticks(x)
    ax.set_xticklabels(df1['arch_plot_label'], fontsize=9.5, fontweight='semibold')
    ax.legend(frameon=True, facecolor='white', edgecolor='#cccccc', fontsize=9.5, loc='upper right')
    ax.grid(True, axis='y', alpha=0.3)
    ax.set_ylim(0, 6500)

    # Annotate bar values and savings
    for i, rect in enumerate(rects1):
        h = rect.get_height()
        saving = df1['cloud_bw_saving_pct'].iloc[i]
        label = f"{h:.1f} KB\n({saving:.1f}% Save)" if saving > 0 else f"{h:.1f} KB\n(Baseline)"
        ax.annotate(label, xy=(rect.get_x() + rect.get_width()/2, h),
                    xytext=(0, 4), textcoords="offset points", ha='center', va='bottom', 
                    fontsize=8, fontweight='bold' if saving > 0 else 'normal')

    for rect in rects2:
        h = rect.get_height()
        ax.annotate(f"{h:.1f} KB", xy=(rect.get_x() + rect.get_width()/2, h),
                    xytext=(0, 3), textcoords="offset points", ha='center', va='bottom', fontsize=7.5)

    plt.tight_layout()
    png_path = os.path.join(GRAPHS_DIR, 'fig2_arch_traffic_comparison.png')
    pdf_path = os.path.join(GRAPHS_DIR, 'fig2_arch_traffic_comparison.pdf')
    plt.savefig(png_path, dpi=300, bbox_inches='tight')
    plt.savefig(pdf_path, bbox_inches='tight')
    plt.close()
    print(f"[Fig 2 Saved] {png_path}")

# ========================================================================
# FIGURE 3: Patient & Sensor Scalability vs. Critical Latency
# ========================================================================
def plot_fig3_scalability_latency(df2):
    fig, ax = plt.subplots(figsize=(6.5, 4.2), dpi=300)

    arch_styles = {
        'Arch A': {'color': PALETTE['ArchA'], 'marker': 'o', 'linestyle': '-', 'label': 'Architecture A (Cloud-Only)'},
        'Arch B': {'color': PALETTE['ArchB'], 'marker': 's', 'linestyle': '--', 'label': 'Architecture B (Fog Forward)'},
        'Arch C': {'color': PALETTE['ArchC'], 'marker': '^', 'linestyle': '-.', 'label': 'Architecture C (Filter & Agg)'},
        'Arch D': {'color': PALETTE['ArchD'], 'marker': 'D', 'linestyle': '-', 'label': 'Architecture D (Priority Fog)'}
    }

    for code, style in arch_styles.items():
        sub = df2[df2['arch_code'] == code].sort_values('total_sensors')
        ax.plot(sub['total_sensors'], sub['critical_latency_ms'], 
                color=style['color'], marker=style['marker'], markersize=7, 
                linestyle=style['linestyle'], linewidth=2, label=style['label'])

    ax.set_yscale('log')
    ax.set_xlabel('Total Sensors Monitored ($4$ per patient)', fontsize=11, fontweight='bold')
    ax.set_ylabel('Critical Alert Delivery Latency (ms) [Log Scale]', fontsize=11, fontweight='bold')
    ax.set_title('Figure 3: Critical Alert Latency vs. System Scale ($N=10 \dots 50$ Patients)',
                 fontsize=11.5, fontweight='bold', pad=12)
    ax.set_xticks([40, 80, 120, 200])
    ax.set_xticklabels(['40 (N=10)', '80 (N=20)', '120 (N=30)', '200 (N=50)'], fontsize=9.5)
    ax.legend(frameon=True, facecolor='white', edgecolor='#cccccc', fontsize=9, loc='center right')
    ax.grid(True, which='both', axis='y', alpha=0.3)
    ax.grid(True, axis='x', alpha=0.2)
    ax.set_ylim(3, 8000)

    ax.yaxis.set_major_formatter(ticker.FuncFormatter(lambda y, _: f'{int(y):,}' if y >= 1 else f'{y}'))

    # Callout highlighting flat Arch D curve
    ax.annotate('Architecture D Invariance:\nFlat 7.13 ms Latency up to 200 Sensors', 
                xy=(120, 7.126), xytext=(80, 25),
                arrowprops=dict(facecolor=PALETTE['ArchD'], arrowstyle='->', lw=1.2),
                fontsize=8.5, fontweight='bold', color=PALETTE['ArchD'],
                bbox=dict(boxstyle="round,pad=0.3", fc="#eafaf1", ec=PALETTE['ArchD'], lw=0.8))

    plt.tight_layout()
    png_path = os.path.join(GRAPHS_DIR, 'fig3_scalability_latency.png')
    pdf_path = os.path.join(GRAPHS_DIR, 'fig3_scalability_latency.pdf')
    plt.savefig(png_path, dpi=300, bbox_inches='tight')
    plt.savefig(pdf_path, bbox_inches='tight')
    plt.close()
    print(f"[Fig 3 Saved] {png_path}")

# ========================================================================
# FIGURE 4: Patient & Sensor Scalability vs. Cloud Traffic Volume
# ========================================================================
def plot_fig4_scalability_cloud_traffic(df2):
    fig, ax = plt.subplots(figsize=(6.5, 4.2), dpi=300)

    arch_styles = {
        'Arch A': {'color': PALETTE['ArchA'], 'marker': 'o', 'linestyle': '-', 'label': 'Architecture A (Cloud-Only)'},
        'Arch B': {'color': PALETTE['ArchB'], 'marker': 's', 'linestyle': ':', 'label': 'Architecture B (Fog Forward)'},
        'Arch C': {'color': PALETTE['ArchC'], 'marker': '^', 'linestyle': '-.', 'label': 'Architecture C (Filter & Agg)'},
        'Arch D': {'color': PALETTE['ArchD'], 'marker': 'D', 'linestyle': '-', 'label': 'Architecture D (Priority Fog)'}
    }

    for code, style in arch_styles.items():
        sub = df2[df2['arch_code'] == code].sort_values('total_sensors')
        ax.plot(sub['total_sensors'], sub['cloud_traffic_kb'], 
                color=style['color'], marker=style['marker'], markersize=7, 
                linestyle=style['linestyle'], linewidth=2, label=style['label'])

    ax.set_xlabel('Total Sensors Monitored ($4$ per patient)', fontsize=11, fontweight='bold')
    ax.set_ylabel('Cloud-Bound WAN Traffic Volume (KB)', fontsize=11, fontweight='bold')
    ax.set_title('Figure 4: WAN Bandwidth Scalability vs. Sensor Count',
                 fontsize=11.5, fontweight='bold', pad=12)
    ax.set_xticks([40, 80, 120, 200])
    ax.set_xticklabels(['40 (N=10)', '80 (N=20)', '120 (N=30)', '200 (N=50)'], fontsize=9.5)
    ax.legend(frameon=True, facecolor='white', edgecolor='#cccccc', fontsize=9.5, loc='upper left')
    ax.grid(True, alpha=0.3)
    ax.set_ylim(0, 11000)

    # Shaded band illustrating bandwidth savings gap at 200 sensors
    a_200 = df2.loc[(df2['arch_code'] == 'Arch A') & (df2['total_sensors'] == 200), 'cloud_traffic_kb'].values[0]
    c_200 = df2.loc[(df2['arch_code'] == 'Arch C') & (df2['total_sensors'] == 200), 'cloud_traffic_kb'].values[0]
    d_200 = df2.loc[(df2['arch_code'] == 'Arch D') & (df2['total_sensors'] == 200), 'cloud_traffic_kb'].values[0]

    ax.annotate(f'77.5% Bandwidth Reduction\n({a_200:,.0f} KB → {c_200:,.0f} KB)', 
                xy=(200, c_200), xytext=(135, 1200),
                arrowprops=dict(facecolor=PALETTE['ArchC'], arrowstyle='->', lw=1.2),
                fontsize=8.5, fontweight='bold', color=PALETTE['ArchC'],
                bbox=dict(boxstyle="round,pad=0.3", fc="#fff5eb", ec=PALETTE['ArchC'], lw=0.8))

    ax.annotate(f'67.0% Bandwidth Reduction\n({a_200:,.0f} KB → {d_200:,.0f} KB)', 
                xy=(200, d_200), xytext=(135, 4500),
                arrowprops=dict(facecolor=PALETTE['ArchD'], arrowstyle='->', lw=1.2),
                fontsize=8.5, fontweight='bold', color=PALETTE['ArchD'],
                bbox=dict(boxstyle="round,pad=0.3", fc="#eafaf1", ec=PALETTE['ArchD'], lw=0.8))

    plt.tight_layout()
    png_path = os.path.join(GRAPHS_DIR, 'fig4_scalability_cloud_traffic.png')
    pdf_path = os.path.join(GRAPHS_DIR, 'fig4_scalability_cloud_traffic.pdf')
    plt.savefig(png_path, dpi=300, bbox_inches='tight')
    plt.savefig(pdf_path, bbox_inches='tight')
    plt.close()
    print(f"[Fig 4 Saved] {png_path}")

# ========================================================================
# FIGURE 5: Window Size Pareto Trade-Off (1x2 Dual Panel)
# ========================================================================
def plot_fig5_window_pareto_tradeoff(df3):
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.5, 4.2), dpi=300)

    # Filter out baseline W=1 if desired for window trend, or include W=2..20
    sub_c = df3[df3['arch_code'] == 'Arch C'].sort_values('window_size')
    sub_d = df3[df3['arch_code'] == 'Arch D'].sort_values('window_size')

    # Panel A: Bandwidth Saving vs Window Size W
    ax1.plot(sub_c['window_size'], sub_c['cloud_bw_saving_pct'], 
             color=PALETTE['ArchC'], marker='^', markersize=8, linewidth=2.2, label='Arch C (Filter & Agg)')
    ax1.plot(sub_d['window_size'], sub_d['cloud_bw_saving_pct'], 
             color=PALETTE['ArchD'], marker='D', markersize=8, linewidth=2.2, label='Arch D (Priority Fog)')

    ax1.set_xlabel('Aggregation Window Size ($W$ Readings)', fontsize=10.5, fontweight='bold')
    ax1.set_ylabel('Cloud Bandwidth Saving (%) vs Arch A', fontsize=10.5, fontweight='bold')
    ax1.set_title('(a) Bandwidth Saving vs. Window Size ($N=20$)', fontsize=11, fontweight='bold')
    ax1.set_xticks([2, 5, 10, 20])
    ax1.set_ylim(30, 100)
    ax1.grid(True, alpha=0.3)
    ax1.legend(frameon=True, facecolor='white', edgecolor='#cccccc', fontsize=9, loc='lower right')

    for _, row in sub_c.iterrows():
        ax1.annotate(f"{row['cloud_bw_saving_pct']:.1f}%", (row['window_size'], row['cloud_bw_saving_pct']),
                     xytext=(0, 5), textcoords="offset points", ha='center', fontsize=8, color=PALETTE['ArchC'], fontweight='semibold')
    for _, row in sub_d.iterrows():
        ax1.annotate(f"{row['cloud_bw_saving_pct']:.1f}%", (row['window_size'], row['cloud_bw_saving_pct']),
                     xytext=(0, -12), textcoords="offset points", ha='center', fontsize=8, color=PALETTE['ArchD'], fontweight='semibold')

    # Panel B: Critical Latency vs Window Size W + SLA Threshold
    ax2.plot(sub_c['window_size'], sub_c['critical_latency_ms'], 
             color=PALETTE['ArchC'], marker='^', markersize=8, linewidth=2.2, label='Arch C (Delayed with W)')
    ax2.plot(sub_d['window_size'], sub_d['critical_latency_ms'], 
             color=PALETTE['ArchD'], marker='D', markersize=8, linewidth=2.2, label='Arch D (Decoupled Bypass)')

    ax2.axhline(y=500.0, color=PALETTE['SLA'], linestyle='--', linewidth=1.5, label='Clinical Emergency SLA (500 ms)')

    ax2.set_yscale('log')
    ax2.set_xlabel('Aggregation Window Size ($W$ Readings)', fontsize=10.5, fontweight='bold')
    ax2.set_ylabel('Critical Reading Latency (ms) [Log Scale]', fontsize=10.5, fontweight='bold')
    ax2.set_title('(b) Critical Vitals Safety vs. Window Size ($N=20$)', fontsize=11, fontweight='bold')
    ax2.set_xticks([2, 5, 10, 20])
    ax2.set_ylim(2, 25000)
    ax2.grid(True, which='both', alpha=0.3)
    ax2.legend(frameon=True, facecolor='white', edgecolor='#cccccc', fontsize=8.5, loc='center left')
    ax2.yaxis.set_major_formatter(ticker.FuncFormatter(lambda y, _: f'{int(y):,}' if y >= 1 else f'{y}'))

    # Annotate Arch D flat decoupling line
    ax2.annotate('Safe Emergency Zone (< 10 ms)\nBypasses Window Completely', 
                 xy=(10, 7.129), xytext=(5, 15),
                 arrowprops=dict(facecolor=PALETTE['ArchD'], arrowstyle='->', lw=1.2),
                 fontsize=8, fontweight='bold', color=PALETTE['ArchD'],
                 bbox=dict(boxstyle="round,pad=0.3", fc="#eafaf1", ec=PALETTE['ArchD'], lw=0.8))

    # Annotate Arch C failure crossing the 500ms line
    ax2.annotate('Clinical Hazard: 10,551 ms Delay\n(SLA Violated by 21x at W=20)', 
                 xy=(20, sub_c.loc[sub_c['window_size']==20, 'critical_latency_ms'].values[0]),
                 xytext=(8, 12000),
                 arrowprops=dict(facecolor=PALETTE['ArchC'], arrowstyle='->', lw=1.2),
                 fontsize=8, fontweight='bold', color=PALETTE['ArchC'],
                 bbox=dict(boxstyle="round,pad=0.3", fc="#fff5eb", ec=PALETTE['ArchC'], lw=0.8))

    plt.suptitle('Figure 5: Aggregation Window ($W$) Sensitivity & Pareto Decoupling Analysis',
                 fontsize=12, fontweight='bold', y=0.98)
    plt.tight_layout()
    png_path = os.path.join(GRAPHS_DIR, 'fig5_window_pareto_tradeoff.png')
    pdf_path = os.path.join(GRAPHS_DIR, 'fig5_window_pareto_tradeoff.pdf')
    plt.savefig(png_path, dpi=300, bbox_inches='tight')
    plt.savefig(pdf_path, bbox_inches='tight')
    plt.close()
    print(f"[Fig 5 Saved] {png_path}")

# ========================================================================
# FIGURE 6: WAN Network Stress vs. Critical Alert SLA Reliability (%)
# ========================================================================
def plot_fig6_network_stress_reliability(df4):
    fig, ax = plt.subplots(figsize=(7.2, 4.4), dpi=300)

    profiles = ['Fiber_Normal', 'WAN_Congested', 'WAN_Severe_Bottleneck', 'WAN_Rural_Degraded']
    profile_labels = [
        'Fiber Normal\n(100ms, 100M)',
        'WAN Congested\n(200ms, 1M)',
        'WAN Bottleneck\n(400ms, 250k)',
        'Rural Degraded\n(600ms, 100k)'
    ]

    x = np.arange(len(profiles))
    width = 0.26

    # Extract reliability for Arch A, C, D
    rel_a = [df4.loc[(df4['network_profile'] == p) & (df4['arch_code'] == 'Arch A'), 'critical_sla_reliability_pct'].values[0] for p in profiles]
    rel_c = [df4.loc[(df4['network_profile'] == p) & (df4['arch_code'] == 'Arch C'), 'critical_sla_reliability_pct'].values[0] for p in profiles]
    rel_d = [df4.loc[(df4['network_profile'] == p) & (df4['arch_code'] == 'Arch D'), 'critical_sla_reliability_pct'].values[0] for p in profiles]

    rects1 = ax.bar(x - width, rel_a, width, label='Arch A (Cloud-Only)', 
                    color=PALETTE['ArchA'], edgecolor='black', linewidth=0.6, alpha=0.9)
    rects2 = ax.bar(x, rel_c, width, label='Arch C (Filter & Agg)', 
                    color=PALETTE['ArchC'], edgecolor='black', linewidth=0.6, alpha=0.9)
    rects3 = ax.bar(x + width, rel_d, width, label='Arch D (Priority-Aware Fog)', 
                    color=PALETTE['ArchD'], edgecolor='black', linewidth=0.6, alpha=0.9)

    ax.set_ylabel(r'Critical Alert SLA Compliance (%) [$\leq 500$ ms]', fontsize=10.5, fontweight='bold')
    ax.set_title('Figure 6: Critical Alert SLA Reliability under WAN Stress ($N=20$)',
                 fontsize=11.5, fontweight='bold', pad=12)
    ax.set_xticks(x)
    ax.set_xticklabels(profile_labels, fontsize=9, fontweight='semibold')
    ax.set_ylim(0, 115)
    ax.legend(frameon=True, facecolor='white', edgecolor='#cccccc', fontsize=9, loc='upper right')
    ax.grid(True, axis='y', alpha=0.3)

    # Annotate bar percentages
    for rect in rects1:
        h = rect.get_height()
        ax.annotate(f"{h:.0f}%", xy=(rect.get_x() + rect.get_width()/2, h),
                    xytext=(0, 3), textcoords="offset points", ha='center', va='bottom', fontsize=8,
                    fontweight='bold' if h == 100 else 'normal')
    for rect in rects2:
        h = rect.get_height()
        ax.annotate(f"{h:.1f}%" if h > 0 else "0%", xy=(rect.get_x() + rect.get_width()/2, h),
                    xytext=(0, 3), textcoords="offset points", ha='center', va='bottom', fontsize=8)
    for rect in rects3:
        h = rect.get_height()
        ax.annotate(f"{h:.0f}%", xy=(rect.get_x() + rect.get_width()/2, h),
                    xytext=(0, 3), textcoords="offset points", ha='center', va='bottom', fontsize=8.5,
                    fontweight='bold', color='darkgreen')

    # Add red highlight box around failure profiles
    ax.axvspan(1.5, 3.5, color='#ffebee', alpha=0.4, zorder=0)
    ax.text(2.5, 60, 'WAN Failure Zone:\nArch A & C Collapse to 0%\nArch D Remains 100%',
            ha='center', va='center', fontsize=8.5, fontweight='bold', color='darkred',
            bbox=dict(boxstyle="round,pad=0.4", fc="white", ec="darkred", lw=0.9))

    plt.tight_layout()
    png_path = os.path.join(GRAPHS_DIR, 'fig6_network_stress_reliability.png')
    pdf_path = os.path.join(GRAPHS_DIR, 'fig6_network_stress_reliability.pdf')
    plt.savefig(png_path, dpi=300, bbox_inches='tight')
    plt.savefig(pdf_path, bbox_inches='tight')
    plt.close()
    print(f"[Fig 6 Saved] {png_path}")

# ========================================================================
# FIGURE 7: WAN Network Stress vs. Critical Reading Latency (ms)
# ========================================================================
def plot_fig7_network_stress_latency(df4):
    fig, ax = plt.subplots(figsize=(7.2, 4.4), dpi=300)

    profiles = ['Fiber_Normal', 'WAN_Congested', 'WAN_Severe_Bottleneck', 'WAN_Rural_Degraded']
    profile_labels = [
        'Fiber Normal\n(100ms, 100M)',
        'WAN Congested\n(200ms, 1M)',
        'WAN Bottleneck\n(400ms, 250k)',
        'Rural Degraded\n(600ms, 100k)'
    ]

    lat_a = [df4.loc[(df4['network_profile'] == p) & (df4['arch_code'] == 'Arch A'), 'critical_latency_ms'].values[0] for p in profiles]
    lat_c = [df4.loc[(df4['network_profile'] == p) & (df4['arch_code'] == 'Arch C'), 'critical_latency_ms'].values[0] for p in profiles]
    lat_d = [df4.loc[(df4['network_profile'] == p) & (df4['arch_code'] == 'Arch D'), 'critical_latency_ms'].values[0] for p in profiles]

    x = np.arange(len(profiles))
    width = 0.26

    rects1 = ax.bar(x - width, lat_a, width, label='Arch A (Cloud-Only)', 
                    color=PALETTE['ArchA'], edgecolor='black', linewidth=0.6, alpha=0.9)
    rects2 = ax.bar(x, lat_c, width, label='Arch C (Filter & Agg)', 
                    color=PALETTE['ArchC'], edgecolor='black', linewidth=0.6, alpha=0.9)
    rects3 = ax.bar(x + width, lat_d, width, label='Arch D (Priority-Aware Fog)', 
                    color=PALETTE['ArchD'], edgecolor='black', linewidth=0.6, alpha=0.9)

    ax.axhline(y=500.0, color=PALETTE['SLA'], linestyle='--', linewidth=1.5, label='Clinical Emergency SLA (500 ms)')

    ax.set_yscale('log')
    ax.set_ylabel('Critical Reading Latency (ms) [Log Scale]', fontsize=10.5, fontweight='bold')
    ax.set_title('Figure 7: Critical Alert Latency under Network Degradation ($N=20$)',
                 fontsize=11.5, fontweight='bold', pad=12)
    ax.set_xticks(x)
    ax.set_xticklabels(profile_labels, fontsize=9, fontweight='semibold')
    ax.set_ylim(2, 8000)
    ax.legend(frameon=True, facecolor='white', edgecolor='#cccccc', fontsize=8.5, loc='upper left')
    ax.grid(True, which='both', axis='y', alpha=0.3)
    ax.yaxis.set_major_formatter(ticker.FuncFormatter(lambda y, _: f'{int(y):,}' if y >= 1 else f'{y}'))

    for rect in rects1:
        h = rect.get_height()
        ax.annotate(f"{h:.0f}", xy=(rect.get_x() + rect.get_width()/2, h),
                    xytext=(0, 2), textcoords="offset points", ha='center', va='bottom', fontsize=7.5)
    for rect in rects2:
        h = rect.get_height()
        ax.annotate(f"{h:.0f}", xy=(rect.get_x() + rect.get_width()/2, h),
                    xytext=(0, 2), textcoords="offset points", ha='center', va='bottom', fontsize=7.5)
    for rect in rects3:
        h = rect.get_height()
        ax.annotate(f"{h:.2f} ms", xy=(rect.get_x() + rect.get_width()/2, h),
                    xytext=(0, 2), textcoords="offset points", ha='center', va='bottom', 
                    fontsize=8, fontweight='bold', color='darkgreen')

    plt.tight_layout()
    png_path = os.path.join(GRAPHS_DIR, 'fig7_network_stress_latency.png')
    pdf_path = os.path.join(GRAPHS_DIR, 'fig7_network_stress_latency.pdf')
    plt.savefig(png_path, dpi=300, bbox_inches='tight')
    plt.savefig(pdf_path, bbox_inches='tight')
    plt.close()
    print(f"[Fig 7 Saved] {png_path}")

# ========================================================================
# TABLES: Processed Summary Tables (Markdown & LaTeX)
# ========================================================================
def generate_summary_tables(df1, df2, df3, df4):
    md_path = os.path.join(PROCESSED_DIR, 'summary_tables.md')
    tex_path = os.path.join(PROCESSED_DIR, 'summary_tables.tex')

    # --- Markdown Generation ---
    md_content = """# Empirical Summary Tables: Topic 19 Healthcare Fog Simulation

This document contains publication-ready summary tables derived dynamically from our Phase 4 experimental datasets.

---

## Table 1: Cross-Architecture Comparison ($N=10$ Patients, $40$ Sensors, $W=5$)
*(Source: `results/csv/exp1_architecture_comparison.csv`)*

| Architecture | Filtered at Fog | Total Delivered | Critical Delivery (%) | Critical Latency (ms) | Avg Latency (ms) | Cloud Traffic (KB) | Cloud BW Saving (%) | Critical SLA Rel. (%) |
|---|---|---|---|---|---|---|---|---|
"""
    for _, r in df1.iterrows():
        md_content += f"| {r['arch_short']} | {r['filtered_count']:,} ({r['filtered_count']*100/r['total_generated']:.1f}%) | {r['delivered_count']:,} | {r['critical_reliability_pct']:.2f}% | **{r['critical_latency_ms']:.3f}** | {r['avg_latency_ms']:.3f} | {r['cloud_traffic_kb']:.2f} | **{r['cloud_bw_saving_pct']:.2f}%** | **{r['critical_sla_reliability_pct']:.2f}%** |\n"

    md_content += """
---

## Table 2: Patient & Sensor Scalability ($N \in \{10, 20, 30, 50\}$)
*(Source: `results/csv/exp2_scalability.csv`)*

| Architecture | Patients ($N$) | Sensors | Total Generated | Critical Latency (ms) | Normal Latency (ms) | Avg Latency (ms) | Cloud Traffic (KB) | Cloud BW Saving (%) | Critical SLA Rel. (%) |
|---|---|---|---|---|---|---|---|---|---|
"""
    for _, r in df2.iterrows():
        md_content += f"| {r['arch_code']} | {r['num_patients']} | {r['total_sensors']} | {r['total_generated']:,} | {r['critical_latency_ms']:.3f} | {r['normal_latency_ms']:.3f} | {r['avg_latency_ms']:.3f} | {r['cloud_traffic_kb']:.2f} | {r['cloud_bw_saving_pct']:.2f}% | {r['critical_sla_reliability_pct']:.2f}% |\n"

    md_content += """
---

## Table 3: Aggregation Window ($W$) Sensitivity ($N=20$, $80$ Sensors)
*(Source: `results/csv/exp3_aggregation_window.csv`)*

| Architecture | Window Size ($W$) | Critical Latency (ms) | Abnormal Latency (ms) | Normal Latency (ms) | Avg Latency (ms) | Cloud Traffic (KB) | Cloud BW Saving (%) | Critical SLA Rel. (%) |
|---|---|---|---|---|---|---|---|---|
"""
    for _, r in df3.iterrows():
        md_content += f"| {r['arch_code']} | {r['window_size']} | **{r['critical_latency_ms']:.3f}** | {r['abnormal_latency_ms']:.3f} | {r['normal_latency_ms']:.3f} | {r['avg_latency_ms']:.3f} | {r['cloud_traffic_kb']:.2f} | **{r['cloud_bw_saving_pct']:.2f}%** | **{r['critical_sla_reliability_pct']:.2f}%** |\n"

    md_content += """
---

## Table 4: WAN Network Degradation & Clinical Reliability ($N=20$, SLA $\le 500$ ms)
*(Source: `results/csv/exp4_network_stress.csv`)*

| Network Profile | Architecture | WAN Latency (ms) | WAN Bw (Kbps) | Critical Latency (ms) | Avg Latency (ms) | Cloud BW Saving (%) | Critical SLA Rel. (%) |
|---|---|---|---|---|---|---|---|
"""
    for _, r in df4.iterrows():
        md_content += f"| {r['network_profile']} | {r['arch_code']} | {r['wan_latency_ms']:.0f} | {r['wan_bandwidth_kbps']:,.0f} | **{r['critical_latency_ms']:.3f}** | {r['avg_latency_ms']:.3f} | {r['cloud_bw_saving_pct']:.2f}% | **{r['critical_sla_reliability_pct']:.2f}%** |\n"

    with open(md_path, 'w', encoding='utf-8') as f:
        f.write(md_content)
    print(f"[Table Saved] {md_path}")

    # --- LaTeX Generation (IEEE Format) ---
    tex_content = r"""% ========================================================================
% IEEE Publication Tables: Fog-Assisted Real-Time Healthcare System (Topic 19)
% ========================================================================

% Table I: Architecture Comparison
\begin{table*}[t]
\centering
\caption{Cross-Architecture Performance Comparison ($N=10$ Patients, $40$ Sensors, Window Size $W=5$)}
\label{tab:arch_comparison}
\begin{tabular}{lrrrrrrr}
\toprule
\textbf{Architecture} & \textbf{Filtered} & \textbf{Delivered} & \textbf{Critical Lat. (ms)} & \textbf{Avg Lat. (ms)} & \textbf{Cloud KB} & \textbf{Cloud Saving} & \textbf{SLA Rel. (\%)} \\
\midrule
"""
    for _, r in df1.iterrows():
        arch_name = r['arch_short'].replace('&', r'\&')
        tex_content += f"{arch_name} & {r['filtered_count']} & {r['delivered_count']} & {r['critical_latency_ms']:.2f} & {r['avg_latency_ms']:.2f} & {r['cloud_traffic_kb']:.1f} & {r['cloud_bw_saving_pct']:.1f}\\% & {r['critical_sla_reliability_pct']:.1f}\\% \\\\\n"

    tex_content += r"""\bottomrule
\end{tabular}
\end{table*}

% Table II: Scalability Sweep
\begin{table*}[t]
\centering
\caption{Patient and Sensor Scalability Analysis ($N \in \{10, 20, 30, 50\}$)}
\label{tab:scalability}
\begin{tabular}{lrrrrrrr}
\toprule
\textbf{Architecture} & \textbf{Patients ($N$)} & \textbf{Sensors} & \textbf{Critical Lat. (ms)} & \textbf{Avg Lat. (ms)} & \textbf{Cloud KB} & \textbf{Cloud Saving} & \textbf{SLA Rel. (\%)} \\
\midrule
"""
    for _, r in df2.iterrows():
        tex_content += f"{r['arch_code']} & {r['num_patients']} & {r['total_sensors']} & {r['critical_latency_ms']:.2f} & {r['avg_latency_ms']:.2f} & {r['cloud_traffic_kb']:.1f} & {r['cloud_bw_saving_pct']:.1f}\\% & {r['critical_sla_reliability_pct']:.1f}\\% \\\\\n"

    tex_content += r"""\bottomrule
\end{tabular}
\end{table*}

% Table III: Aggregation Window Sensitivity
\begin{table*}[t]
\centering
\caption{Aggregation Window ($W$) Sensitivity and Pareto Trade-Off ($N=20$, $80$ Sensors)}
\label{tab:window_sensitivity}
\begin{tabular}{lrrrrrrr}
\toprule
\textbf{Architecture} & \textbf{Window Size ($W$)} & \textbf{Critical Lat. (ms)} & \textbf{Abnormal Lat. (ms)} & \textbf{Normal Lat. (ms)} & \textbf{Cloud KB} & \textbf{Cloud Saving} & \textbf{SLA Rel. (\%)} \\
\midrule
"""
    for _, r in df3.iterrows():
        tex_content += f"{r['arch_code']} & {r['window_size']} & {r['critical_latency_ms']:.2f} & {r['abnormal_latency_ms']:.2f} & {r['normal_latency_ms']:.2f} & {r['cloud_traffic_kb']:.1f} & {r['cloud_bw_saving_pct']:.1f}\\% & {r['critical_sla_reliability_pct']:.1f}\\% \\\\\n"

    tex_content += r"""\bottomrule
\end{tabular}
\end{table*}

% Table IV: WAN Network Stress
\begin{table*}[t]
\centering
\caption{WAN Network Stress Profiles and Clinical Emergency SLA Compliance ($N=20$, SLA $\leq 500$ ms)}
\label{tab:network_stress}
\begin{tabular}{llrrrrr}
\toprule
\textbf{Profile} & \textbf{Architecture} & \textbf{WAN Latency (ms)} & \textbf{WAN Bw (Kbps)} & \textbf{Critical Lat. (ms)} & \textbf{Cloud Saving} & \textbf{SLA Rel. (\%)} \\
\midrule
"""
    for _, r in df4.iterrows():
        prof_name = r['network_profile'].replace('_', r'\_')
        tex_content += f"{prof_name} & {r['arch_code']} & {r['wan_latency_ms']:.0f} & {r['wan_bandwidth_kbps']:.0f} & {r['critical_latency_ms']:.2f} & {r['cloud_bw_saving_pct']:.1f}\\% & {r['critical_sla_reliability_pct']:.1f}\\% \\\\\n"

    tex_content += r"""\bottomrule
\end{tabular}
\end{table*}
"""

    with open(tex_path, 'w', encoding='utf-8') as f:
        f.write(tex_content)
    print(f"[Table Saved] {tex_path}")

def main():
    print("========================================================================")
    print(" Starting Phase 5 Analysis & IEEE Publication Graph Generation          ")
    print("========================================================================")
    df1, df2, df3, df4 = load_data()
    print(f"[Data Loaded] Exp 1 ({len(df1)} rows), Exp 2 ({len(df2)} rows), Exp 3 ({len(df3)} rows), Exp 4 ({len(df4)} rows)")

    plot_fig1_latency_by_severity(df1)
    plot_fig2_network_traffic(df1)
    plot_fig3_scalability_latency(df2)
    plot_fig4_scalability_cloud_traffic(df2)
    plot_fig5_window_pareto_tradeoff(df3)
    plot_fig6_network_stress_reliability(df4)
    plot_fig7_network_stress_latency(df4)

    generate_summary_tables(df1, df2, df3, df4)

    print("========================================================================")
    print(" Phase 5 Analysis Completed Successfully! All Figures & Tables Saved.    ")
    print("========================================================================")

if __name__ == '__main__':
    main()
