$ErrorActionPreference = "Stop"

$dirs = @(
    "src/baseline",
    "src/fog_forwarding",
    "src/fog_filtering",
    "src/priority_aware",
    "experiments/architecture_comparison",
    "experiments/scalability",
    "experiments/aggregation_window",
    "experiments/network_stress",
    "results/raw",
    "results/processed",
    "results/csv",
    "results/graphs",
    "analysis",
    "research",
    "report",
    "presentation"
)

foreach ($d in $dirs) {
    if (-not (Test-Path $d)) {
        New-Item -ItemType Directory -Path $d -Force | Out-Null
        Write-Host "Created directory: $d"
    } else {
        Write-Host "Directory already exists: $d"
    }
}
