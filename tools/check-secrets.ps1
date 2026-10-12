# check-secrets.ps1 — скан ТРЕКАЕМЫХ git-файлов на утечки (секреты, ключи, личные данные,
# внутренние адреса). Запуск: pwsh tools/check-secrets.ps1
# Возврат: exit 1 при находках, exit 0 если чисто. Содержит только generic-шаблоны,
# реальных секретов в самом скрипте нет.
[CmdletBinding()]
param(
    [string[]]$Paths,
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'

# Расширения текстовых файлов, которые имеет смысл сканировать
$textExt = @(
    '.md', '.kt', '.kts', '.gradle', '.properties', '.json', '.txt', '.xml',
    '.ps1', '.psm1', '.py', '.mjs', '.js', '.ts', '.yml', '.yaml', '.toml',
    '.html', '.htm', '.css', '.sh', '.bat', '.cmd', '.cfg', '.ini'
)

# Паттерн -> человеческое описание. Generic, без реальных значений.
$rules = @(
    @{ Name = 'SpriteCook API-key';  Re = 'sc_live_[A-Za-z0-9]{6,}' }
    @{ Name = 'OpenAI-style key';    Re = 'sk-[A-Za-z0-9]{16,}' }
    @{ Name = 'PEM private key';     Re = '-----BEGIN [A-Z ]*PRIVATE KEY-----' }
    @{ Name = 'RSA key (base64)';    Re = 'MIIE[A-Za-z0-9+/]{20,}' }
    @{ Name = 'RuStore keyId';       Re = 'keyId=(?!0{6})\d{7,}' }
    @{ Name = 'OpenCode session';    Re = 'st_[A-Za-z0-9_.-]{20,}' }
    @{ Name = 'Bearer token';        Re = 'Bearer\s+[A-Za-z0-9._-]{20,}' }
    @{ Name = 'LAN IPv4 (192.168)';  Re = '192\.168\.\d{1,3}\.\d{1,3}' }
    @{ Name = '10.x LAN IPv4';       Re = '(?<![\d.])10\.\d{1,3}\.\d{1,3}\.\d{1,3}(?![\d.])' }
    @{ Name = 'Windows user path';   Re = '[A-Za-z]:\\+Users\\+[A-Za-z0-9._-]+' }
    @{ Name = 'E-mail address';      Re = '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}'; Allow = '^(andrey@440hz\.ru|.+@example\.[a-z]+|.+@users\.noreply.*)$' }
)

if ($Paths -and $Paths.Count -gt 0) {
    $files = $Paths
} else {
    $files = git ls-files
}

$findings = New-Object System.Collections.Generic.List[string]
$scanned = 0

foreach ($f in $files) {
    if (-not (Test-Path -LiteralPath $f -PathType Leaf)) { continue }
    $ext = [System.IO.Path]::GetExtension($f).ToLowerInvariant()
    if ($ext -and ($textExt -notcontains $ext)) { continue }

    try {
        $content = Get-Content -LiteralPath $f -Raw -ErrorAction Stop
    } catch { continue }
    if ($null -eq $content) { continue }
    if ($content.IndexOf([char]0) -ge 0) { continue }   # бинарный — пропускаем
    $scanned++

    $lines = $content -split "`r?`n"
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        foreach ($rule in $rules) {
            foreach ($m in [regex]::Matches($line, $rule.Re)) {
                $val = $m.Value
                $isAllowed = $false
                if ($rule.ContainsKey('Allow') -and $rule.Allow) {
                    if ($val -match $rule.Allow) { $isAllowed = $true }
                }
                if (-not $isAllowed) {
                    $findings.Add(("{0}:{1}  [{2}]  {3}" -f $f, ($i + 1), $rule.Name, $line.Trim()))
                }
            }
        }
    }
}

if ($findings.Count -gt 0) {
    Write-Host "НАЙДЕНЫ возможные утечки ($($findings.Count)):" -ForegroundColor Red
    $findings | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
    exit 1
}

if (-not $Quiet) {
    Write-Host "check-secrets: OK ($scanned файлов проверено, утечек нет)." -ForegroundColor Green
}
exit 0
