#!/usr/bin/env python3
"""
dashboard/server.py
========================================================================
Lightweight Localhost Web Server for Topic 19 iFogSim2 Simulation Demo.
Runs on http://localhost:8000 (with port fallback if occupied).

Endpoints:
- GET  /             -> Serves dashboard/index.html
- GET  /api/metrics  -> Returns preloaded Phase 4 metrics & latest run stats
- POST /api/run      -> Executes real Java iFogSim simulation via build_and_run.ps1
- GET  /api/run      -> Same as POST (allows simple query parameters)
========================================================================
"""

import os
import sys
import json
import csv
import subprocess
import time
from urllib.parse import urlparse, parse_qs
from http.server import HTTPServer, BaseHTTPRequestHandler

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DASHBOARD_DIR = os.path.join(BASE_DIR, 'dashboard')
CSV_DIR = os.path.join(BASE_DIR, 'results', 'csv')
PORT = 8000

def load_csv_metrics():
    """Load baseline metrics from Phase 4 CSVs and any recent individual summaries."""
    metrics_data = {
        "baseline_comparison": [],
        "latest_summaries": {}
    }

    # 1. Load Exp 1 baseline comparison
    exp1_file = os.path.join(CSV_DIR, 'exp1_architecture_comparison.csv')
    if os.path.exists(exp1_file):
        with open(exp1_file, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                metrics_data["baseline_comparison"].append(row)

    # 2. Load latest individual architecture summaries
    summary_files = {
        "A": "arch_a_baseline_summary.csv",
        "B": "arch_b_forwarding_summary.csv",
        "C": "arch_c_filtering_summary.csv",
        "D": "arch_d_priority_summary.csv",
        "E": "arch_e_multiward_summary.csv"
    }
    for code, filename in summary_files.items():
        fpath = os.path.join(CSV_DIR, filename)
        if os.path.exists(fpath):
            summary = {}
            with open(fpath, 'r', encoding='utf-8') as f:
                reader = csv.reader(f)
                header = next(reader, None)
                for r in reader:
                    if len(r) >= 2:
                        summary[r[0]] = r[1]
            metrics_data["latest_summaries"][code] = summary

    # 3. Load Exp 5 Multi-Ward Failover data
    exp5_file = os.path.join(CSV_DIR, 'exp5_multi_ward_failover.csv')
    if os.path.exists(exp5_file):
        metrics_data["exp5_failover"] = []
        with open(exp5_file, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                metrics_data["exp5_failover"].append(row)

    return metrics_data

class DashboardHandler(BaseHTTPRequestHandler):
    def _send_json(self, data, status=200):
        body = json.dumps(data, indent=2).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
        self.wfile.write(body)

    def _send_html(self, html_content, status=200):
        body = html_content.encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'text/html; charset=utf-8')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path in ['/', '/index.html']:
            index_path = os.path.join(DASHBOARD_DIR, 'index.html')
            if os.path.exists(index_path):
                with open(index_path, 'r', encoding='utf-8') as f:
                    self._send_html(f.read())
            else:
                self._send_html("<h1>dashboard/index.html not found</h1>", status=404)

        elif path == '/api/metrics':
            try:
                data = load_csv_metrics()
                self._send_json({"status": "success", "data": data})
            except Exception as e:
                self._send_json({"status": "error", "message": str(e)}, status=500)

        elif path == '/api/run':
            # Allow running via GET query string for easy browser testing
            params = parse_qs(parsed.query)
            arch = params.get('arch', ['D'])[0].upper()
            patients = params.get('patients', ['10'])[0]
            window = params.get('window', ['5'])[0]
            self.execute_simulation(arch, patients, window)

        else:
            # Serve other static files in dashboard if any
            local_path = os.path.join(DASHBOARD_DIR, path.lstrip('/'))
            if os.path.exists(local_path) and os.path.isfile(local_path):
                with open(local_path, 'rb') as f:
                    content = f.read()
                self.send_response(200)
                if path.endswith('.js'):
                    self.send_header('Content-Type', 'application/javascript')
                elif path.endswith('.css'):
                    self.send_header('Content-Type', 'text/css')
                elif path.endswith('.png'):
                    self.send_header('Content-Type', 'image/png')
                self.send_header('Content-Length', str(len(content)))
                self.end_headers()
                self.wfile.write(content)
            else:
                self.send_error(404, "Not Found")

    def do_POST(self):
        parsed = urlparse(self.path)
        if parsed.path == '/api/run':
            content_length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(content_length).decode('utf-8') if content_length > 0 else '{}'
            try:
                data = json.loads(body)
            except Exception:
                data = {}

            arch = data.get('arch', 'D').upper()
            patients = str(data.get('patients', 10))
            window = str(data.get('window', 5))
            self.execute_simulation(arch, patients, window)
        else:
            self.send_error(404, "Not Found")

    def execute_simulation(self, arch, patients, window):
        # Validate parameters
        if arch not in ['A', 'B', 'C', 'D', 'E', 'ALL']:
            self._send_json({"status": "error", "message": f"Invalid architecture: {arch}. Must be A, B, C, D, E, or ALL."}, status=400)
            return

        valid_patients = ['10', '20', '30', '50']
        if str(patients) not in valid_patients:
            self._send_json({"status": "error", "message": f"Invalid patients: {patients}. Choose from {valid_patients}."}, status=400)
            return

        valid_windows = ['1', '2', '5', '10', '20']
        if str(window) not in valid_windows:
            self._send_json({"status": "error", "message": f"Invalid window size: {window}. Choose from {valid_windows}."}, status=400)
            return

        cmd = [
            'powershell', '-ExecutionPolicy', 'Bypass',
            '-File', 'build_and_run.ps1',
            '-Arch', arch,
            '-NumPatients', str(patients),
            '-WindowSize', str(window)
        ]

        start_time = time.time()
        try:
            proc = subprocess.run(
                cmd,
                cwd=BASE_DIR,
                capture_output=True,
                text=True,
                timeout=120
            )
            elapsed_sec = round(time.time() - start_time, 2)
            stdout = proc.stdout
            stderr = proc.stderr
            returncode = proc.returncode

            # Read the corresponding summary file if single architecture
            summary_metrics = {}
            if arch in ['A', 'B', 'C', 'D', 'E']:
                summary_map = {
                    "A": "arch_a_baseline_summary.csv",
                    "B": "arch_b_forwarding_summary.csv",
                    "C": "arch_c_filtering_summary.csv",
                    "D": "arch_d_priority_summary.csv",
                    "E": "arch_e_multiward_summary.csv"
                }
                fpath = os.path.join(CSV_DIR, summary_map[arch])
                if os.path.exists(fpath):
                    with open(fpath, 'r', encoding='utf-8') as f:
                        reader = csv.reader(f)
                        next(reader, None)
                        for r in reader:
                            if len(r) >= 2:
                                summary_metrics[r[0]] = r[1]

            # Re-read all baseline metrics
            all_metrics = load_csv_metrics()

            self._send_json({
                "status": "success" if returncode == 0 else "simulation_failed",
                "arch": arch,
                "num_patients": int(patients),
                "total_sensors": int(patients) * 4,
                "window_size": int(window),
                "elapsed_seconds": elapsed_sec,
                "summary": summary_metrics,
                "stdout_tail": "\n".join(stdout.splitlines()[-25:]),
                "return_code": returncode,
                "all_metrics": all_metrics
            })
        except subprocess.TimeoutExpired:
            self._send_json({"status": "error", "message": "Simulation timed out after 120 seconds."}, status=504)
        except Exception as e:
            self._send_json({"status": "error", "message": str(e)}, status=500)

def run_server(port=PORT):
    for p in range(port, port + 10):
        try:
            server = HTTPServer(('', p), DashboardHandler)
            print(f"========================================================================")
            print(f" iFogSim2 Simulation Dashboard Server running at:                     ")
            print(f" >>> http://localhost:{p} <<<                                        ")
            print(f"========================================================================")
            print(f"Serving dashboard/index.html with live iFogSim2 execution engine.")
            print(f"Press Ctrl+C to terminate.")
            server.serve_forever()
            break
        except OSError as e:
            if p == port + 9:
                print(f"Error: Unable to bind to ports {port} through {p}: {e}")
                sys.exit(1)
            continue

if __name__ == '__main__':
    port = int(sys.argv[1]) if len(sys.argv) > 1 else PORT
    run_server(port)
