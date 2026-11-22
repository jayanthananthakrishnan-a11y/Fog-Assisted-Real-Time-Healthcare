# ========================================================================
# start_dashboard.ps1
# One-Click Launcher for Topic 19 iFogSim2 Simulation Dashboard
# ========================================================================

$workspaceRoot = $PSScriptRoot
if ((Split-Path $workspaceRoot -Leaf) -eq "dashboard") {
    $workspaceRoot = Split-Path $workspaceRoot -Parent
}
Set-Location $workspaceRoot

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host " Starting Topic 19 Healthcare Fog Simulation Dashboard (Localhost:8000) " -ForegroundColor Green
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "Open http://localhost:8000 in your browser to view the 3D visualizer."

& python -u dashboard/server.py 8000
