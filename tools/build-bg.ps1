# Неблокирующая пересборка билдов CATS AND MOUSES.
#
# Запускает Gradle отдельным процессом и сразу возвращает управление: агент/терминал
# не «зависает» на долгой сборке. Результат — по логам (BUILD SUCCESSFUL / BUILD FAILED).
#
# Примеры:
#   pwsh tools/build-bg.ps1                       # build + дистрибутив + APK (debug/release) + AAB
#   pwsh tools/build-bg.ps1 build                 # только тесты/сборка
#   pwsh tools/build-bg.ps1 android:assembleDebug # конкретная задача

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Tasks
)

$root = Split-Path -Parent $PSScriptRoot
if (-not $Tasks -or $Tasks.Count -eq 0) {
    $Tasks = @("build", "lwjgl3:installDist", "android:assembleDebug", "android:assembleRelease", "android:bundleRelease")
}

$logDir = Join-Path $root "lwjgl3\build\build-logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$outLog = Join-Path $logDir "build-out.log"
$gradle = Join-Path $root "gradlew.bat"

# Лаунчер .bat: избавляет от проблем с кавычками в путях с пробелами при запуске через cmd.
$bat = Join-Path $logDir "run-build.bat"
$lines = @(
    "@echo off",
    "cd /d `"$root`"",
    "call `"$gradle`" $($Tasks -join ' ') --console=plain > `"$outLog`" 2>&1"
)
Set-Content -Path $bat -Value $lines -Encoding ASCII

# WScript.Shell.Run стартует процесс через ShellExecute и не остаётся в дереве вызывающего
# шелла — управление возвращается сразу (в отличие от Start-Process). bat запускаем через
# `cmd /c "путь"` (одинарные кавычки вокруг пути — WScript не понимает `""путь""`).
$shell = New-Object -ComObject WScript.Shell
$shell.Run('cmd.exe /c "' + $bat + '"', 0, $false) | Out-Null

Write-Output "Сборка запущена в фоне: gradlew $($Tasks -join ' ')"
Write-Output "Лог: $outLog"
