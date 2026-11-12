# Verify PID, process name and start time without requiring WMI access.
$ErrorActionPreference = 'Stop'
foreach ($name in @('backend','frontend')) {
 $pidFile = Join-Path $PSScriptRoot ".runtime/$name.pid"
 if (Test-Path -LiteralPath $pidFile) {
  $record = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
  $process = Get-Process -Id $record.Id -ErrorAction SilentlyContinue
  if ($process -and $process.ProcessName -eq $record.Name -and $process.StartTime.ToUniversalTime().Ticks -eq $record.StartTicks) {
   Stop-Process -Id $record.Id -Force
   Wait-Process -Id $record.Id -Timeout 10 -ErrorAction SilentlyContinue
   Write-Host "Stopped $name"
  }
  Remove-Item -LiteralPath $pidFile
 }
}
