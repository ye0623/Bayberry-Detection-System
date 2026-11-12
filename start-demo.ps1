param([switch]$NoBrowser)
# 本地开发演示启动器：启动两个隐藏后台进程，日志位于 .runtime。
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$runtimeDir = Join-Path $PSScriptRoot '.runtime'
New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
foreach ($port in @(8080,5173)) {
 if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { throw "Port $port is occupied. Run stop-demo.ps1 or release the port first." }
}
$mavenExe = (Get-Command mvn.cmd -ErrorAction Stop).Source
# A newly installed Node.js may not be visible in an existing terminal's PATH.
# Resolve it without changing the user's permanent environment variables.
$nodeCommand = Get-Command node.exe -ErrorAction SilentlyContinue
$nodeCandidates = @()
if ($nodeCommand) { $nodeCandidates += $nodeCommand.Source }
foreach ($nodeHomeDir in @($env:NODE_HOME, $env:NVM_SYMLINK)) {
 if ($nodeHomeDir) { $nodeCandidates += Join-Path $nodeHomeDir 'node.exe' }
}
$nodeCandidates += @(
 "$env:ProgramFiles\nodejs\node.exe",
 "${env:ProgramFiles(x86)}\nodejs\node.exe",
 "$env:LOCALAPPDATA\Programs\nodejs\node.exe",
 "$env:USERPROFILE\scoop\apps\nodejs-lts\current\node.exe",
 "$env:USERPROFILE\scoop\apps\nodejs\current\node.exe",
 "$env:USERPROFILE\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe"
)
$nodeExe = $nodeCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
if (-not $nodeExe) { throw 'Node.js was not found. Install Node.js 20.19+ (including npm), or set NODE_HOME to its installation directory.' }
$nodeDir = Split-Path -Parent $nodeExe
$npmCandidates = @( (Join-Path $nodeDir 'node_modules/npm/bin/npm-cli.js') )
$npmCommand = Get-Command npm.cmd -ErrorAction SilentlyContinue
if ($npmCommand) { $npmCandidates += Join-Path (Split-Path -Parent $npmCommand.Source) 'node_modules/npm/bin/npm-cli.js' }
$npmCandidates += @(
 "$env:ProgramFiles\nodejs\node_modules\npm\bin\npm-cli.js",
 "${env:ProgramFiles(x86)}\nodejs\node_modules\npm\bin\npm-cli.js"
)
$npmCliPath = $npmCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
if (-not $npmCliPath) { throw 'npm was not found. Reinstall Node.js with the npm component enabled.' }
$env:Path = "$nodeDir;$env:Path"
Write-Host "Node.js: $nodeExe"
Write-Host "npm CLI: $npmCliPath"
if (-not (Test-Path -LiteralPath 'frontend/node_modules')) {
 Write-Host 'Installing frontend dependencies (first run may take a few minutes)...'
 & $nodeExe $npmCliPath --prefix frontend install --no-audit --no-fund
 if ($LASTEXITCODE -ne 0) { throw 'npm install failed' }
}
Write-Host '[1/3] Building backend with Maven (first run may download dependencies)...'
& $mavenExe -s maven-settings.xml -f backend/pom.xml package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw 'Maven build failed; see console output.' }
$backendDir = Join-Path $PSScriptRoot 'backend'
$frontendDir = Join-Path $PSScriptRoot 'frontend'
$javaExe = (Get-Command java.exe -ErrorAction Stop).Source
Write-Host '[2/3] Starting backend; waiting for login API...'
$backendProcess = Start-Process -FilePath $javaExe -ArgumentList '-jar target/bayberry-iot-1.0.0.jar' -WorkingDirectory $backendDir -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeDir 'backend.log') -RedirectStandardError (Join-Path $runtimeDir 'backend-error.log')
Set-Content -LiteralPath (Join-Path $runtimeDir 'backend.pid') -Value (@{Id=$backendProcess.Id;StartTicks=$backendProcess.StartTime.ToUniversalTime().Ticks;Name=$backendProcess.ProcessName} | ConvertTo-Json -Compress)
$ready = $false
for ($i=0; $i -lt 60; $i++) {
 if ($backendProcess.HasExited) { throw 'Backend failed. See .runtime/backend.log and backend-error.log.' }
 try { $null = Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:8080/api/auth/login' -Method POST -ContentType 'application/json' -Body '{"username":"farmer","password":"123456"}'; $ready=$true; break } catch { Start-Sleep -Milliseconds 1000 }
}
if (-not $ready) { throw 'Backend startup timed out; check .runtime/backend.log.' }
Write-Host '[3/3] Starting frontend; waiting for web page...'
$frontendProcess = Start-Process -FilePath $nodeExe -ArgumentList 'node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5173' -WorkingDirectory $frontendDir -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeDir 'frontend.log') -RedirectStandardError (Join-Path $runtimeDir 'frontend-error.log')
Set-Content -LiteralPath (Join-Path $runtimeDir 'frontend.pid') -Value (@{Id=$frontendProcess.Id;StartTicks=$frontendProcess.StartTime.ToUniversalTime().Ticks;Name=$frontendProcess.ProcessName} | ConvertTo-Json -Compress)
$frontendReady=$false
for ($i=0; $i -lt 30; $i++) {
 if ($frontendProcess.HasExited) { throw 'Frontend failed; check .runtime/frontend-error.log.' }
 try { $null=Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:5173' -TimeoutSec 2; $frontendReady=$true; break } catch { Start-Sleep -Milliseconds 500 }
}
if (-not $frontendReady) { throw 'Frontend startup timed out.' }
Write-Host 'Demo ready: http://127.0.0.1:5173'
Write-Host 'farmer / 123456 ; admin / 123456'
if (-not $NoBrowser) { Start-Process 'http://127.0.0.1:5173' }
