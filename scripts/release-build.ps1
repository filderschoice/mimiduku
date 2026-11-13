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
    固定で使う versionCode（整数）。省略時は現在値から 1 つ上げる。現在値より小さい値は受け付けない。

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
    [int]$VersionCode,
    [string]$VersionName
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$versionFile = Join-Path $PSScriptRoot "version.properties"

$currentVersionCode = 1
$currentVersionName = "0.1.0"

if (Test-Path $versionFile) {
    foreach ($line in Get-Content $versionFile) {
        if ($line -match "^VERSION_CODE=(.+)$") { $currentVersionCode = [int]$Matches[1] }
        if ($line -match "^VERSION_NAME=(.+)$") { $currentVersionName = $Matches[1] }
    }
} else {
    Write-Warning "scripts/version.properties が見つからないため、既定値（VERSION_CODE=1・VERSION_NAME=0.1.0）から始めます。"
}

if ($PSBoundParameters.ContainsKey("VersionCode")) {
    $newVersionCode = $VersionCode
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
