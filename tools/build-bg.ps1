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
$errLog = Join-Path $logDir "build-err.log"

$gradle = Join-Path $root "gradlew.bat"
$argLine = "/c `"$gradle`" " + ($Tasks -join " ") + " --console=plain"
Start-Process -FilePath "cmd.exe" -ArgumentList $argLine `
    -WorkingDirectory $root -WindowStyle Hidden `
    -RedirectStandardOutput $outLog -RedirectStandardError $errLog

Write-Output "Сборка запущена в фоне: gradlew $($Tasks -join ' ')"
Write-Output "Логи: $outLog / $errLog"
