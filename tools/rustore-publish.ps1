<#
.SYNOPSIS
    Публикация и сопровождение приложения CATS AND MOUSES в RuStore через Public API.

.DESCRIPTION
    Авторизация по приватному ключу RuStore (JWE), работа с версиями (черновик,
    загрузка APK/AAB, иконка, скриншоты, настройки, модерация, публикация, архив)
    и отзывами (список, CSV, ответ, рейтинг). Зависимостей нет: подпись — .NET RSA,
    HTTP — Invoke-RestMethod.

.PARAMETER Action
    token, apps, status, release, publish, settings, draft-delete, archive,
    icon, screens, reviews, reviews-csv, reply, rating

.PARAMETER Config
    JSON с параметрами версии (по умолчанию store/rustore/version.json).

.PARAMETER KeyFile
    Файл с keyId и privateKey (по умолчанию rustore-api.properties, gitignored).

.PARAMETER File
    APK/AAB для release, иконка для icon.

.PARAMETER VersionId
    ID версии. Если не задан — подбирается автоматически по действию.

.PARAMETER PublishType
    Для settings: MANUAL | INSTANTLY | DELAYED.

.PARAMETER PublishDate
    Для settings (DELAYED): дата вида 2026-10-01T12:00:00+03:00.

.PARAMETER PartialValue
    Для settings: 5 | 10 | 25 | 50 | 75 | 100.

.PARAMETER Screens
    Для screens: файл или каталог со скриншотами (jpg/png).

.PARAMETER Orientation
    Для screens: LANDSCAPE (по умолчанию) или PORTRAIT.

.PARAMETER ScreenType
    Для screens: SCREENSHOT | TABLET_SCREENSHOT | TV_SCREENSHOT.

.PARAMETER DeleteId
    Для screens: id удаляемого скриншота.

.PARAMETER Page
    Для reviews: номер страницы (с 0).

.PARAMETER Size
    Для reviews: размер страницы (по умолчанию 50, максимум 100).

.PARAMETER CommentId
    Для reviews/reply: id отзыва.

.PARAMETER Message
    Для reply: текст ответа (1..500 символов).

.PARAMETER From
    Для reviews-csv: дата с (YYYY-MM-DD).

.PARAMETER To
    Для reviews-csv: дата по (YYYY-MM-DD).

.PARAMETER Out
    Для reviews-csv: путь сохранения (по умолчанию reviews.csv).
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet(
        "token", "apps", "status", "release", "publish", "settings", "draft-delete",
        "archive", "icon", "screens", "reviews", "reviews-csv", "reply", "rating"
    )]
    [string]$Action = "status",

    [string]$Config = "store/rustore/version.json",
    [string]$KeyFile = "rustore-api.properties",
    [string]$File = "",
    [int]$VersionId = 0,
    [ValidateSet("", "MANUAL", "INSTANTLY", "DELAYED")]
    [string]$PublishType = "",
    [string]$PublishDate = "",
    [ValidateSet(0, 5, 10, 25, 50, 75, 100)]
    [int]$PartialValue = 0,
    [string]$Screens = "",
    [ValidateSet("LANDSCAPE", "PORTRAIT")]
    [string]$Orientation = "LANDSCAPE",
    [ValidateSet("SCREENSHOT", "TABLET_SCREENSHOT", "TV_SCREENSHOT")]
    [string]$ScreenType = "SCREENSHOT",
    [string]$DeleteId = "",
    [int]$Page = 0,
    [int]$Size = 50,
    [long]$CommentId = 0,
    [string]$Message = "",
    [string]$From = "",
    [string]$To = "",
    [string]$Out = "",
    [switch]$Help
)

$ErrorActionPreference = "Stop"
$BaseUrl = "https://public-api.rustore.ru"

function Show-Help {
    Write-Host @"
RuStore publish (CATS AND MOUSES)

  pwsh tools/rustore-publish.ps1 <action> [options]

Actions:
  token          проверить авторизацию (получить JWE-токен)
  apps           список приложений (appStatus, versionCode)
  status         список версий приложения
  release        черновик -> загрузка APK/AAB -> отправка на модерацию
  publish        ручная публикация версии после модерации
  settings       тип/дата/процент публикации (-PublishType, -PublishDate, -PartialValue)
  draft-delete   удаление черновика (-VersionId)
  archive        архивация версии (-VersionId)
  icon           загрузка иконки (-File, по умолчанию store/rustore/icon_512.png)
  screens        список без -Screens; загрузка с -Screens <файл|каталог>; удаление с -DeleteId
  reviews        список отзывов (-Page, -Size, -CommentId)
  reviews-csv    выгрузка отзывов в CSV (-From, -To, -Out)
  reply          ответ на отзыв (-CommentId, -Message)
  rating         рейтинг приложения

ВАЖНО: действия, меняющие состояние в RuStore (release, publish, settings,
draft-delete, archive, icon, screens с -Screens/-DeleteId, reply), выполняются
только с явного одобрения пользователя. Read-only: token, apps, status,
reviews, reviews-csv, rating.

Common:
  -Config <path>     JSON параметров версии (store/rustore/version.json)
  -KeyFile <path>    файл с keyId/privateKey (rustore-api.properties)
  -VersionId <n>     id версии (обычно подбирается автоматически)
  -File <path>       APK/AAB для release, иконка для icon

Файл ключа ($KeyFile), формат:
  keyId=<KEY_ID>
  privateKey=<PRIVATE_KEY>...

Где взять: RuStore Консоль -> Разработчик/Компания -> API RuStore -> Создать ключ.
"@
}

function Read-Secrets {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Не найден файл ключа '$Path'. Создай его (см. -Help)."
    }
    $h = @{}
    foreach ($raw in Get-Content -LiteralPath $Path) {
        $line = $raw.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { continue }
        $i = $line.IndexOf("=")
        if ($i -lt 1) { continue }
        $h[$line.Substring(0, $i).Trim()] = $line.Substring($i + 1).Trim()
    }
    if (-not $h["keyId"] -or -not $h["privateKey"]) {
        throw "В '$Path' должны быть заданы keyId и privateKey."
    }
    return $h
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{},
        $Body = $null,
        $Form = $null,
        [switch]$Raw
    )
    try {
        if ($Form) {
            return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers -Form $Form
        }
        if ($Body) {
            return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers `
                -ContentType "application/json" -Body $Body
        }
        if ($Raw) {
            return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers
        }
        return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers
    } catch {
        $detail = $_.ErrorDetails.Message
        if (-not $detail) { $detail = $_.Exception.Message }
        throw "$Method $Uri`n$detail"
    }
}

function New-Token {
    param([string]$KeyId, [string]$PrivateKeyBase64)
    $timestamp = [DateTimeOffset]::Now.ToString(
        "yyyy-MM-dd'T'HH:mm:ss.fffzzz",
        [System.Globalization.CultureInfo]::InvariantCulture
    )
    $message = "$KeyId$timestamp"
    $rsa = [System.Security.Cryptography.RSA]::Create()
    try {
        $read = 0
        $rsa.ImportPkcs8PrivateKey([Convert]::FromBase64String($PrivateKeyBase64), [ref]$read)
        $signature = $rsa.SignData(
            [Text.Encoding]::UTF8.GetBytes($message),
            [System.Security.Cryptography.HashAlgorithmName]::SHA512,
            [System.Security.Cryptography.RSASignaturePadding]::Pkcs1
        )
    } finally {
        $rsa.Dispose()
    }
    $body = @{
        keyId     = $KeyId
        timestamp = $timestamp
        signature = [Convert]::ToBase64String($signature)
    } | ConvertTo-Json -Compress

    $resp = Invoke-Api -Method Post -Uri "$BaseUrl/public/auth/" -Body $body
    if ($resp.code -ne "OK") { throw "auth: $($resp.message)" }
    return $resp.body.jwe
}

function Get-Config {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { throw "Не найден конфиг '$Path'." }
    return (Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json)
}

function Get-Versions {
    param([string]$Pkg, [hashtable]$Headers, [string]$Statuses = "")
    $uri = "$BaseUrl/public/v1/application/$Pkg/version?size=100"
    if ($Statuses) { $uri += "&versionStatuses=$Statuses" }
    $resp = Invoke-Api -Method Get -Uri $uri -Headers $Headers
    if ($resp.code -ne "OK") { throw "version: $($resp.message)" }
    return @($resp.body.content)
}

function Resolve-VersionId {
    param([string]$Pkg, [hashtable]$Headers, [string]$Statuses = "")
    if ($VersionId -gt 0) { return $VersionId }
    $list = Get-Versions -Pkg $Pkg -Headers $Headers -Statuses $Statuses
    if ($list.Count -eq 0) {
        $hint = if ($Statuses) { " со статусом $Statuses" } else { "" }
        throw "Нет версий$hint. Укажи -VersionId."
    }
    return ($list | Sort-Object versionId -Descending | Select-Object -First 1).versionId
}

if ($Help) { Show-Help; return }

$secrets = Read-Secrets -Path $KeyFile
$token = New-Token -KeyId $secrets["keyId"] -PrivateKeyBase64 $secrets["privateKey"]
$headers = @{ "Public-Token" = $token }

switch ($Action) {
    "token" {
        Write-Host "Авторизация OK, токен получен ($($token.Substring(0, 16))...)"
    }

    "apps" {
        $resp = Invoke-Api -Method Get -Uri "$BaseUrl/public/v1/application" -Headers $headers
        if ($resp.code -ne "OK") { throw "application: $($resp.message)" }
        $resp.body.content | Format-Table appId, packageName, appStatus, versionCode, versionName -AutoSize
    }

    "status" {
        $cfg = Get-Config -Path $Config
        Get-Versions -Pkg $cfg.packageName -Headers $headers |
            Format-Table versionId, versionCode, versionName, versionStatus, publishType -AutoSize
    }

    "release" {
        $cfg = Get-Config -Path $Config
        $pkg = $cfg.packageName
        $target = if ($File) { $File } else { $cfg.apk }
        if (-not (Test-Path -LiteralPath $target)) { throw "Не найден файл для загрузки: $target" }

        $draft = $cfg.draft | ConvertTo-Json -Depth 10
        $created = Invoke-Api -Method Post -Uri "$BaseUrl/public/v1/application/$pkg/version" -Headers $headers -Body $draft
        if ($created.code -ne "OK") { throw "create-draft: $($created.message)" }
        $vid = $created.body
        Write-Host "Черновик создан: versionId=$vid"

        $ext = [System.IO.Path]::GetExtension($target).ToLowerInvariant()
        if ($ext -eq ".aab") {
            $uri = "$BaseUrl/public/v1/application/$pkg/version/$vid/aab"
        } else {
            $uri = "$BaseUrl/public/v1/application/$pkg/version/$vid/apk?isMainApk=true&servicesType=Unknown"
        }
        $uploaded = Invoke-Api -Method Post -Uri $uri -Headers $headers -Form @{ file = Get-Item -LiteralPath $target }
        if ($uploaded.code -ne "OK") { throw "file-upload: $($uploaded.message)" }
        Write-Host "Файл загружен: $target"

        $committed = Invoke-Api -Method Post -Uri "$BaseUrl/public/v1/application/$pkg/version/$vid/commit?priorityUpdate=0" -Headers $headers
        if ($committed.code -ne "OK") { throw "commit: $($committed.message)" }
        Write-Host "Версия $vid отправлена на модерацию."
    }

    "publish" {
        $cfg = Get-Config -Path $Config
        $pkg = $cfg.packageName
        $vid = Resolve-VersionId -Pkg $pkg -Headers $headers -Statuses "READY_FOR_PUBLICATION"
        $resp = Invoke-Api -Method Post -Uri "$BaseUrl/public/v1/application/$pkg/version/$vid/publish" -Headers $headers
        if ($resp.code -ne "OK") { throw "publish: $($resp.message)" }
        Write-Host "Версия $vid опубликована."
    }

    "settings" {
        $cfg = Get-Config -Path $Config
        $pkg = $cfg.packageName
        $vid = Resolve-VersionId -Pkg $pkg -Headers $headers
        $body = @{}
        if ($PublishType) { $body["publishType"] = $PublishType }
        if ($PublishDate) { $body["publishDateTime"] = $PublishDate }
        if ($PartialValue -gt 0) { $body["partialValue"] = $PartialValue }
        if ($body.Count -eq 0) { throw "Укажи -PublishType, -PublishDate и/или -PartialValue." }
        $json = $body | ConvertTo-Json -Compress
        $resp = Invoke-Api -Method Post -Uri "$BaseUrl/public/v1/application/$pkg/version/$vid/publish-settings" -Headers $headers -Body $json
        if ($resp.code -ne "OK") { throw "settings: $($resp.message)" }
        Write-Host "Настройки версии $vid изменены: $json"
    }

    "draft-delete" {
        $cfg = Get-Config -Path $Config
        $pkg = $cfg.packageName
        $vid = Resolve-VersionId -Pkg $pkg -Headers $headers -Statuses "DRAFT"
        $resp = Invoke-Api -Method Delete -Uri "$BaseUrl/public/v1/application/$pkg/version/$vid" -Headers $headers
        if ($resp.code -ne "OK") { throw "draft-delete: $($resp.message)" }
        Write-Host "Черновик $vid удалён."
    }

    "archive" {
        $cfg = Get-Config -Path $Config
        $pkg = $cfg.packageName
        $vid = Resolve-VersionId -Pkg $pkg -Headers $headers
        $resp = Invoke-Api -Method Post -Uri "$BaseUrl/public/v1/application/$pkg/version/$vid/archive" -Headers $headers
        if ($resp.code -ne "OK") { throw "archive: $($resp.message)" }
        Write-Host "Версия $vid переведена в архив."
    }

    "icon" {
        $cfg = Get-Config -Path $Config
        $pkg = $cfg.packageName
        $vid = Resolve-VersionId -Pkg $pkg -Headers $headers
        $icon = if ($File) { $File } else { "store/rustore/icon_512.png" }
        if (-not (Test-Path -LiteralPath $icon)) { throw "Не найдена иконка: $icon" }
        $resp = Invoke-Api -Method Post -Uri "$BaseUrl/public/v1/application/$pkg/version/$vid/image/icon" -Headers $headers -Form @{ file = Get-Item -LiteralPath $icon }
        if ($resp.code -ne "OK") { throw "icon: $($resp.message)" }
        Write-Host "Иконка загружена: $icon"
    }

    "screens" {
        $cfg = Get-Config -Path $Config
        $pkg = $cfg.packageName
        $vid = Resolve-VersionId -Pkg $pkg -Headers $headers
        $baseUri = "$BaseUrl/public/v2/application/$pkg/version/$vid/image/screenshot"

        if ($DeleteId) {
            $resp = Invoke-Api -Method Delete -Uri "$baseUri/$DeleteId" -Headers $headers
            if ($resp.code -ne "OK") { throw "screens-delete: $($resp.message)" }
            Write-Host "Скриншот $DeleteId удалён."
        } elseif ($Screens) {
            $files = @()
            if (Test-Path -LiteralPath $Screens -PathType Container) {
                $files = Get-ChildItem -LiteralPath $Screens -File |
                    Where-Object { $_.Extension -in ".jpg", ".jpeg", ".png" } |
                    Sort-Object Name
            } else {
                $files = @(Get-Item -LiteralPath $Screens)
            }
            if ($files.Count -eq 0) { throw "Не найдены jpg/png в '$Screens'." }
            $i = 0
            foreach ($f in $files) {
                $uri = "$baseUri/$Orientation/$i/$ScreenType"
                $resp = Invoke-Api -Method Post -Uri $uri -Headers $headers -Form @{ file = $f }
                if ($resp.code -ne "OK") { throw "screens-upload ($($f.Name)): $($resp.message)" }
                Write-Host "Скриншот [$i] загружен: $($f.Name)"
                $i++
            }
        } else {
            $resp = Invoke-Api -Method Get -Uri "${baseUri}?size=$Size&page=$Page" -Headers $headers
            if ($resp.code -ne "OK") { throw "screens: $($resp.message)" }
            $list = @($resp.body.content)
            if ($list.Count -eq 0) { Write-Host "Скриншотов нет."; return }
            foreach ($s in ($list | Sort-Object ordinal)) {
                Write-Host ("[{0}] id={1} {2}/{3} {4}" -f $s.ordinal, $s.id, $s.type, $s.orientation, $s.fileUrl)
            }
        }
    }

    "reviews" {
        $cfg = Get-Config -Path $Config
        $pkg = $cfg.packageName
        $uri = "$BaseUrl/public/v1/application/$pkg/comment?page=$Page&size=$Size"
        if ($CommentId -gt 0) { $uri += "&id=$CommentId" }
        $resp = Invoke-Api -Method Get -Uri $uri -Headers $headers
        if ($resp.code -ne "OK") { throw "reviews: $($resp.message)" }
        $list = @($resp.body)
        if ($list.Count -eq 0) { Write-Host "Отзывов пока нет."; return }
        $list | Format-Table commentId, appRating, userName, commentDate, commentText -AutoSize
    }

    "reviews-csv" {
        $cfg = Get-Config -Path $Config
        $pkg = $cfg.packageName
        if (-not $From -or -not $To) { throw "Для reviews-csv нужны -From и -To (YYYY-MM-DD)." }
        $target = if ($Out) { $Out } else { "store/rustore/reviews_${From}_${To}.csv" }
        $resp = Invoke-Api -Method Get -Uri "$BaseUrl/public/v1/application/$pkg/comment/export?from=$From&to=$To" -Headers $headers
        $resp | Out-File -LiteralPath $target -Encoding UTF8
        Write-Host "Отзывы сохранены: $target"
    }

    "reply" {
        $cfg = Get-Config -Path $Config
        $pkg = $cfg.packageName
        if ($CommentId -le 0) { throw "Для reply нужен -CommentId." }
        if (-not $Message) { throw "Для reply нужен -Message." }
        $body = @{ message = $Message } | ConvertTo-Json -Compress
        $resp = Invoke-Api -Method Post -Uri "$BaseUrl/public/v1/application/$pkg/feedback?commentId=$CommentId" -Headers $headers -Body $body
        if ($resp.code -ne "OK") { throw "reply: $($resp.message)" }
        Write-Host "Ответ отправлен: id=$($resp.body.id)"
    }

    "rating" {
        $cfg = Get-Config -Path $Config
        $pkg = $cfg.packageName
        $resp = Invoke-Api -Method Get -Uri "$BaseUrl/public/v1/application/$pkg/comment/statistic" -Headers $headers
        if ($resp.code -ne "OK") { throw "rating: $($resp.message)" }
        $resp.body
    }
}
