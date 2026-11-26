param(
    [string]$Arch = "A",
    [string]$NumPatients = "10",
    [string]$WindowSize = "5",
    [string]$WanLatency = "100.0",
    [string]$WanBw = "10000.0"
)

$ErrorActionPreference = "Stop"

$workspaceRoot = $PSScriptRoot
Set-Location $workspaceRoot

# 1. Verify simulator build exists
$simOut = "simulator/iFogSim/out/production/iFogSim"
if (-not (Test-Path $simOut)) {
    Write-Host "Compiling iFogSim simulator dependencies first..."
    & powershell -ExecutionPolicy Bypass -File "simulator/build.ps1"
}

# 2. Prepare custom output directory
$customOut = "out/production/healthcare"
if (-not (Test-Path $customOut)) {
    New-Item -ItemType Directory -Path $customOut -Force | Out-Null
}

# 3. Assemble classpath
$jarFiles = Get-ChildItem -Path "simulator/iFogSim/jars" -Filter '*.jar' -Recurse | ForEach-Object {
    $_.FullName.Substring($workspaceRoot.Length + 1).Replace('\', '/')
}
$compileClasspath = "$simOut;" + ($jarFiles -join ';')

# 4. Gather all custom sources across packages
$sourcesFile = "custom_sources.txt"
$sourceFiles = Get-ChildItem -Path "src" -Filter '*.java' -Recurse | ForEach-Object {
    $_.FullName.Substring($workspaceRoot.Length + 1).Replace('\', '/')
}
[System.IO.File]::WriteAllLines((Join-Path $workspaceRoot $sourcesFile), $sourceFiles, (New-Object System.Text.UTF8Encoding $false))

Write-Host "Compiling $($sourceFiles.Count) custom healthcare source files..."
$javacArgs = @("-encoding", "UTF-8", "-cp", $compileClasspath, "-d", $customOut, "@$sourcesFile")
$proc = Start-Process -FilePath "javac" -ArgumentList $javacArgs -NoNewWindow -Wait -PassThru
if ($proc.ExitCode -ne 0) {
    Write-Host "Custom code compilation failed with exit code: $($proc.ExitCode)"
    exit $proc.ExitCode
}
Write-Host "Custom code compiled successfully!"

$runClasspath = "$customOut;$compileClasspath"

function Run-Architecture([string]$targetArch) {
    Write-Host ""
    Write-Host "========================================================================"
    Write-Host " >>> EXECUTING ARCHITECTURE $targetArch (Patients: $NumPatients, Window: $WindowSize) <<<"
    Write-Host "========================================================================"
    
    $mainClass = switch ($targetArch.ToUpper()) {
        "A" { "baseline.HealthcareCloudOnly" }
        "B" { "fog_forwarding.HealthcareFogForwarding" }
        "C" { "fog_filtering.HealthcareFogFiltering" }
        "D" { "priority_aware.HealthcarePriorityFog" }
        "E" { "multi_ward.HealthcareMultiWardFailover" }
        "COMPREHENSIVE" { "experiments.ComprehensiveExperimentRunner" }
        Default { throw "Unknown Architecture: $targetArch" }
    }

    & java -cp $runClasspath $mainClass $NumPatients $WindowSize $WanLatency $WanBw
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Architecture $targetArch execution failed with exit code: $LASTEXITCODE"
        exit $LASTEXITCODE
    }
}

if ($Arch.ToUpper() -eq "ALL") {
    Run-Architecture "A"
    Run-Architecture "B"
    Run-Architecture "C"
    Run-Architecture "D"
    Run-Architecture "E"

    Write-Host ""
    Write-Host "========================================================================"
    Write-Host "             CROSS-ARCHITECTURE COMPARISON SUMMARY                     "
    Write-Host "========================================================================"
    
    $summaryFiles = @(
        @{ Arch = "Architecture A (Cloud-Only)"; File = "results/csv/arch_a_baseline_summary.csv" },
        @{ Arch = "Architecture B (Fog Forwarding)"; File = "results/csv/arch_b_forwarding_summary.csv" },
        @{ Arch = "Architecture C (Fog Filtering & Aggregation)"; File = "results/csv/arch_c_filtering_summary.csv" },
        @{ Arch = "Architecture D (Priority-Aware Fog)"; File = "results/csv/arch_d_priority_summary.csv" },
        @{ Arch = "Architecture E (Multi-Ward Failover)"; File = "results/csv/arch_e_multiward_summary.csv" }
    )

    $metricsTable = @()
    foreach ($item in $summaryFiles) {
        if (Test-Path $item.File) {
            $lines = Get-Content $item.File
            $data = @{}
            foreach ($line in $lines) {
                $parts = $line.Split(',')
                if ($parts.Length -ge 2) {
                    $data[$parts[0].Trim()] = $parts[1].Trim()
                }
            }
            $metricsTable += [PSCustomObject]@{
                Architecture = $item.Arch
                Delivered = $data["total_delivered"]
                Filtered = $data["filtered_at_fog"]
                AvgLatency_ms = $data["avg_latency_ms"]
                CriticalLatency_ms = $data["avg_critical_latency_ms"]
                P95Latency_ms = $data["p95_latency_ms"]
                CloudTraffic_KB = $data["cloud_bound_traffic_kb"]
                CloudSaving_Pct = $data["cloud_bandwidth_saving_percent"]
                CriticalDelivery = $data["critical_delivery_rate"] + "%"
            }
        }
    }

    $metricsTable | Format-Table -AutoSize
    Write-Host "========================================================================"
} else {
    Run-Architecture $Arch
}
