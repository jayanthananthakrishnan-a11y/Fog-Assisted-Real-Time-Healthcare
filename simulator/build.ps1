$ErrorActionPreference = "Stop"

$workspaceRoot = $PSScriptRoot | Split-Path -Parent
$simDir = "$PSScriptRoot/iFogSim"
Set-Location $simDir

$outDir = "out/production/iFogSim"
if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

# Collect all jar paths
$jarFiles = Get-ChildItem -Path "jars" -Filter '*.jar' -Recurse | ForEach-Object {
    $_.FullName.Substring($simDir.Length + 1).Replace('\', '/')
}
$classpath = ($jarFiles -join ';')

# Write relative paths without BOM
$sourcesFile = "$simDir/sources.txt"
$sourceFiles = Get-ChildItem -Path "src" -Filter '*.java' -Recurse | ForEach-Object {
    $_.FullName.Substring($simDir.Length + 1).Replace('\', '/')
}
[System.IO.File]::WriteAllLines($sourcesFile, $sourceFiles, (New-Object System.Text.UTF8Encoding $false))

$sourceCount = $sourceFiles.Count
Write-Host "Compiling $sourceCount Java source files..."
Write-Host "Classpath: $classpath"

$javacArgs = @(
    "-encoding", "UTF-8",
    "-cp", $classpath,
    "-d", $outDir,
    "@sources.txt"
)

$proc = Start-Process -FilePath "javac" -ArgumentList $javacArgs -NoNewWindow -Wait -PassThru
Write-Host "Javac finished with exit code: $($proc.ExitCode)"
if ($proc.ExitCode -ne 0) {
    Write-Host "Compilation failed!"
    exit $proc.ExitCode
} else {
    Write-Host "Compilation succeeded!"
}
