# Welltech Mobile Agent - Teste USB/ADB
# Protocolo 1.0 | Windows PowerShell 5.1+

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Port = 37183
$BaseUrl = "http://127.0.0.1:$Port/api/v1"
$OutputDir = Join-Path $PSScriptRoot "resultado"
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null

function Write-Step([string]$Text) {
    Write-Host ""
    Write-Host "==> $Text" -ForegroundColor Cyan
}

function Find-Adb {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $candidates = @(
        (Join-Path $PSScriptRoot "platform-tools\adb.exe"),
        (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"),
        "C:\platform-tools\adb.exe",
        "C:\Android\platform-tools\adb.exe"
    )

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) { return $candidate }
    }
    return $null
}

function Save-DiagnosticLog([string]$Adb, [string]$Serial) {
    try {
        $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
        $path = Join-Path $OutputDir "welltech-agent-logcat-$stamp.txt"
        & $Adb -s $Serial logcat -d -v time 2>$null |
            Select-String -Pattern "FATAL EXCEPTION|AndroidRuntime|com\.welltech\.mobile|Welltech|welltech" |
            ForEach-Object { $_.Line } |
            Set-Content -Path $path -Encoding UTF8
        Write-Host "Log técnico salvo em: $path" -ForegroundColor Yellow
    } catch {
        Write-Host "Não foi possível salvar o logcat automaticamente." -ForegroundColor DarkYellow
    }
}

function Wait-AgentHealth([int]$Seconds) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        try {
            return Invoke-RestMethod -Uri "$BaseUrl/health" -Method Get -TimeoutSec 2
        } catch {
            Start-Sleep -Milliseconds 900
        }
    }
    return $null
}

function Invoke-AgentGet([string]$Path, [hashtable]$Headers) {
    return Invoke-RestMethod -Uri "$BaseUrl$Path" -Method Get -Headers $Headers -TimeoutSec 10
}

function Invoke-AgentPost([string]$Path, [hashtable]$Headers, $Body) {
    $json = if ($null -eq $Body) { "{}" } else { $Body | ConvertTo-Json -Depth 8 -Compress }
    return Invoke-RestMethod -Uri "$BaseUrl$Path" -Method Post -Headers $Headers -ContentType "application/json" -Body $json -TimeoutSec 10
}

$adb = Find-Adb
if (-not $adb) {
    Write-Host ""
    Write-Host "ADB não encontrado." -ForegroundColor Red
    Write-Host "Coloque a pasta oficial platform-tools ao lado deste teste ou instale o Android Platform Tools."
    Read-Host "Pressione ENTER para fechar"
    exit 2
}

Write-Step "Iniciando ADB"
& $adb start-server | Out-Null

Write-Step "Detectando celular Android"
$rawDevices = & $adb devices
$deviceRows = @()
foreach ($line in $rawDevices) {
    if ($line -match '^(\S+)\s+(device|unauthorized|offline)$') {
        $deviceRows += [pscustomobject]@{ Serial = $Matches[1]; State = $Matches[2] }
    }
}

$unauthorized = @($deviceRows | Where-Object State -eq "unauthorized")
if ($unauthorized.Count -gt 0) {
    Write-Host "O celular está conectado, mas ainda não autorizou a depuração USB." -ForegroundColor Yellow
    Write-Host "Desbloqueie o celular e toque em PERMITIR na mensagem de depuração USB."
    Read-Host "Depois pressione ENTER para tentar novamente"
    $rawDevices = & $adb devices
    $deviceRows = @()
    foreach ($line in $rawDevices) {
        if ($line -match '^(\S+)\s+(device|unauthorized|offline)$') {
            $deviceRows += [pscustomobject]@{ Serial = $Matches[1]; State = $Matches[2] }
        }
    }
}

$ready = @($deviceRows | Where-Object State -eq "device")
if ($ready.Count -eq 0) {
    Write-Host "Nenhum Android autorizado foi encontrado." -ForegroundColor Red
    Read-Host "Pressione ENTER para fechar"
    exit 3
}
if ($ready.Count -gt 1) {
    Write-Host "Há mais de um Android conectado. Para este teste inicial, deixe apenas um aparelho conectado." -ForegroundColor Yellow
    Read-Host "Pressione ENTER para fechar"
    exit 4
}

$serial = $ready[0].Serial
Write-Host "Dispositivo: $serial" -ForegroundColor Green

Write-Step "Localizando Welltech Mobile Agent"
$packages = (& $adb -s $serial shell pm list packages 2>$null | Out-String)
$packageName = $null
if ($packages -match 'package:com\.welltech\.mobile(\r?\n|$)') {
    $packageName = "com.welltech.mobile"
} elseif ($packages -match 'package:com\.welltech\.mobile\.debug(\r?\n|$)') {
    $packageName = "com.welltech.mobile.debug"
}

if (-not $packageName) {
    Write-Host "Welltech Mobile Agent não está instalado neste aparelho." -ForegroundColor Red
    Read-Host "Pressione ENTER para fechar"
    exit 5
}

Write-Host "Pacote encontrado: $packageName" -ForegroundColor Green

Write-Step "Abrindo e preparando o Agent no celular"
& $adb -s $serial shell am start -W -a com.welltech.mobile.action.DESKTOP_PREPARE -n "$packageName/com.welltech.mobile.MainActivity" --es welltech_desktop_source desktop --ez welltech_prepare_agent true 2>$null | Out-Null
Start-Sleep -Seconds 2

try { & $adb -s $serial forward --remove "tcp:$Port" 2>$null | Out-Null } catch {}
& $adb -s $serial forward "tcp:$Port" "tcp:$Port" | Out-Null

Write-Host ""
Write-Host "NO CELULAR:" -ForegroundColor Yellow
Write-Host "1. Deixe o Welltech Mobile Agent aberto."
Write-Host "2. Toque em ABRIR PAREAMENTO."
Write-Host "O computador vai detectar automaticamente quando o Agent estiver pronto."
Write-Host ""

$health = Wait-AgentHealth 70
if ($null -eq $health) {
    Write-Host "O Agent não respondeu pela porta local." -ForegroundColor Red
    Write-Host "Vou salvar um log técnico para descobrir se houve crash ou bloqueio do Android."
    Save-DiagnosticLog $adb $serial
    try { & $adb -s $serial forward --remove "tcp:$Port" 2>$null | Out-Null } catch {}
    Read-Host "Pressione ENTER para fechar"
    exit 6
}

Write-Host "Agent encontrado: $($health.service) | Protocolo $($health.protocolMajor).$($health.protocolMinor)" -ForegroundColor Green
if (-not $health.readyForPairing) {
    Write-Host "O Agent respondeu, mas o pareamento ainda não está aberto." -ForegroundColor Yellow
    Write-Host "Toque em ABRIR PAREAMENTO no celular."
}

$code = Read-Host "Digite o código de 6 dígitos mostrado no celular"
if ($code -notmatch '^\d{6}$') {
    Write-Host "Código inválido. Digite exatamente 6 números." -ForegroundColor Red
    try { & $adb -s $serial forward --remove "tcp:$Port" 2>$null | Out-Null } catch {}
    exit 7
}

$token = $null
$sessionStarted = $false

try {
    Write-Step "Pareando computador e celular"
    $pair = Invoke-AgentPost "/pair" @{} @{ code = $code }
    $token = [string]$pair.token
    if ([string]::IsNullOrWhiteSpace($token)) { throw "O Agent não retornou token de sessão." }

    $headers = @{ Authorization = "Bearer $token" }

    Write-Step "Iniciando sessão de diagnóstico"
    $session = Invoke-AgentPost "/session/start" $headers @{}
    $sessionStarted = $true

    Write-Step "Lendo capabilities"
    $caps = Invoke-AgentGet "/capabilities" $headers

    Write-Step "Lendo dispositivo"
    $device = Invoke-AgentGet "/device" $headers

    Write-Step "Lendo bateria"
    $battery = Invoke-AgentGet "/battery" $headers

    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $resultPath = Join-Path $OutputDir "welltech-agent-teste-$stamp.json"

    $safeResult = [ordered]@{
        test = "welltech-mobile-agent-usb-adb"
        protocol = "$($health.protocolMajor).$($health.protocolMinor)"
        package = $packageName
        adbSerial = $serial
        capturedAt = (Get-Date).ToString("o")
        health = $health
        session = $session
        capabilities = $caps
        device = $device
        battery = $battery
        security = @{
            tokenPersisted = $false
            transport = "ADB forward -> 127.0.0.1:$Port"
        }
    }

    $safeResult | ConvertTo-Json -Depth 20 | Set-Content -Path $resultPath -Encoding UTF8

    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host " TESTE WELLTECH: APROVADO" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green

    try {
        $m = $device.payload.manufacturer.value
        $model = $device.payload.model.value
        $android = $device.payload.androidVersion.value
        $level = $battery.payload.levelPercent.value
        Write-Host "Aparelho: $m $model"
        Write-Host "Android: $android"
        Write-Host "Bateria: $level%"
    } catch {}

    Write-Host "Relatório salvo em: $resultPath"
    Write-Host "O token de sessão NÃO foi gravado no arquivo." -ForegroundColor DarkGreen
}
catch {
    Write-Host ""
    Write-Host "TESTE NÃO CONCLUÍDO: $($_.Exception.Message)" -ForegroundColor Red
    Save-DiagnosticLog $adb $serial
}
finally {
    if ($token -and $sessionStarted) {
        try {
            $headers = @{ Authorization = "Bearer $token" }
            Invoke-AgentPost "/session/stop" $headers @{} | Out-Null
        } catch {}
    }
    try { & $adb -s $serial forward --remove "tcp:$Port" 2>$null | Out-Null } catch {}
}

Write-Host ""
Read-Host "Pressione ENTER para fechar"
