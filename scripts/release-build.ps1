<#
.SYNOPSIS
    GitHub 版の署名付き release APK と、Play 版の署名付き release AAB（Android App Bundle）をまとめてビルドする（BL-075）。

.DESCRIPTION
    sesami-wear の scripts/release-build.ps1 と同じ方式。scripts/version.properties に記録した版を基準に、
    versionCode を 1 つ上げてビルドする。パラメータを指定した場合は、その値をそのまま使う（上げない）。
    ビルドが成功したときだけ version.properties を新しい値へ更新する。

    GitHub 版（com.mimiduku.mobile.full）と Play 版（com.mimiduku.mobile）は applicationId が異なるため、
    versionCode・versionName は両方の版で共通の1系列とする（sesami-wear の mobile と wear のような別系列は持たない）。

    署名は local.properties の RELEASE_STORE_FILE などのキー（README.md「リリースビルド」）から mobile/build.gradle.kts が
    読む。未設定の場合、成果物は署名なしのままになる。鍵の作成・保管と本スクリプトの実行はユーザーが行う
    （エージェントは鍵を扱わず、release ビルドを実行しない。guardrails 12.5）。

.PARAMETER VersionCode
    固定で使う versionCode（1 以上の整数）。省略時は現在値から 1 つ上げる。現在値より小さい値、0 以下・整数でない値は
    ビルドに進まずに中止する。version.properties に読めない行がある場合も、行番号を示して中止する。

.PARAMETER VersionName
    固定で使う versionName（文字列。両方の版で共通）。省略時は現在値のまま。

.EXAMPLE
    scripts\release-build.bat
    versionCode を 1 つ上げてビルドする（versionName は変えない）。

.EXAMPLE
    scripts\release-build.bat -VersionCode 2 -VersionName 0.2.0
    指定した値をそのまま使ってビルドする。
#>
param(
    # [int] で受けると "1.5" が丸められ、数値でない値は英語の引数エラーになるため、文字列で受けて下で検査する。
    [string]$VersionCode,
    [string]$VersionName
)

$ErrorActionPreference = "Stop"

# Google Play が受け付ける versionCode の上限。
$maxVersionCode = 2100000000

# versionCode として使える値（1 以上・上限以下の整数）なら整数を、使えなければ $null を返す。
function ConvertTo-VersionCode([string]$text) {
    if ($text -notmatch "^\s*[0-9]+\s*$") { return $null }
    $value = 0L
    if (-not [long]::TryParse($text.Trim(), [ref]$value)) { return $null }
    if ($value -lt 1 -or $value -gt $maxVersionCode) { return $null }
    return [int]$value
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$versionFile = Join-Path $PSScriptRoot "version.properties"

$currentVersionCode = 1
$currentVersionName = "0.1.0"

if (Test-Path $versionFile) {
    # 読めない行を黙って読み飛ばすと、壊れた記録を基準に版を上げてしまうため、行番号を示して中止する。
    $lineNumber = 0
    foreach ($line in Get-Content $versionFile) {
        $lineNumber++
        if ($line -match "^\s*$" -or $line -match "^\s*#") { continue }
        if ($line -match "^VERSION_CODE=(.*)$") {
            $parsed = ConvertTo-VersionCode $Matches[1]
            if ($null -eq $parsed) {
                throw "scripts/version.properties の $lineNumber 行目を読めません（VERSION_CODE は 1 以上 $maxVersionCode 以下の整数）: $line"
            }
            $currentVersionCode = $parsed
        } elseif ($line -match "^VERSION_NAME=(.+)$") {
            $currentVersionName = $Matches[1].Trim()
        } else {
            throw "scripts/version.properties の $lineNumber 行目を読めません（VERSION_CODE=<整数> か VERSION_NAME=<版> の形で書く）: $line"
        }
    }
} else {
    Write-Warning "scripts/version.properties が見つからないため、既定値（VERSION_CODE=1・VERSION_NAME=0.1.0）から始めます。"
}

if ($PSBoundParameters.ContainsKey("VersionCode")) {
    $newVersionCode = ConvertTo-VersionCode $VersionCode
    if ($null -eq $newVersionCode) {
        throw "-VersionCode（$VersionCode）は使えません。1 以上 $maxVersionCode 以下の整数を指定してください。"
    }
} else {
    $newVersionCode = $currentVersionCode + 1
}

if ($PSBoundParameters.ContainsKey("VersionName")) {
    $newVersionName = $VersionName
} else {
    $newVersionName = $currentVersionName
}

if ($newVersionCode -lt $currentVersionCode) {
    throw "versionCode（$newVersionCode）が現在値（$currentVersionCode）より小さいため中止します。配布済みの版より小さい versionCode では更新できません。"
}

Write-Host "リリースビルドを開始します: versionCode=$newVersionCode / versionName=$newVersionName"

$gradlewPath = Join-Path $repoRoot "gradlew.bat"
if (-not (Test-Path $gradlewPath)) {
    throw "gradlew.bat が見つかりません: $gradlewPath"
}

$gradleArgs = @(
    ":mobile:assembleGithubRelease"
    ":mobile:bundlePlayRelease"
    "-PappVersionCode=$newVersionCode"
    "-PappVersionName=$newVersionName"
    "--no-daemon"
)

Push-Location $repoRoot
try {
    & $gradlewPath @gradleArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle のビルドが失敗しました（終了コード: $LASTEXITCODE）。version.properties は更新していません。"
    }
} finally {
    Pop-Location
}

# Set-Content は環境依存の改行を付けて無意味な差分が出るため、LF・BOM なしの UTF-8 で書き出す（sesami-wear と同じ）。
$versionContent = "VERSION_CODE=$newVersionCode`nVERSION_NAME=$newVersionName`n"
[System.IO.File]::WriteAllText($versionFile, $versionContent, (New-Object System.Text.UTF8Encoding $false))

Write-Host ""
Write-Host "リリースビルドが完了しました: versionCode=$newVersionCode / versionName=$newVersionName"
Write-Host "  GitHub 版の APK: mobile/build/outputs/apk/github/release/mobile-github-release.apk"
Write-Host "    → GitHub Releases などで配布する"
Write-Host "  Play 版の AAB: mobile/build/outputs/bundle/playRelease/mobile-play-release.aab"
Write-Host "    → Play Console へアップロードする"
Write-Host ""
Write-Host "署名の設定（local.properties）が無い場合、上の成果物は署名なしのままです（APK は mobile-github-release-unsigned.apk）。"
Write-Host "手順は README.md「リリースビルド」を参照してください。"
