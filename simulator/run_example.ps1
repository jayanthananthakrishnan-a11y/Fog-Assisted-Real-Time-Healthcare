param(
    [string]$ClassName = "org.fog.test.perfeval.VRGameFog"
)

$ErrorActionPreference = "Stop"

$simDir = "$PSScriptRoot/iFogSim"
Set-Location $simDir

$jarFiles = Get-ChildItem -Path "jars" -Filter '*.jar' -Recurse | ForEach-Object {
    $_.FullName.Substring($simDir.Length + 1).Replace('\', '/')
}
$classpath = "out/production/iFogSim;" + ($jarFiles -join ';')

Write-Host "Running $ClassName..."
$javaArgs = @(
    "-cp", $classpath,
    $ClassName
)

$proc = Start-Process -FilePath "java" -ArgumentList $javaArgs -NoNewWindow -Wait -PassThru
Write-Host "Java process finished with exit code: $($proc.ExitCode)"
if ($proc.ExitCode -ne 0) {
    exit $proc.ExitCode
}
