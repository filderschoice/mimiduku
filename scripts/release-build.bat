@echo off
rem リリースビルド(GitHub 版の署名付き APK と Play 版の署名付き AAB)を実行するバッチファイル(BL-075)。
rem sesami-wear の scripts/release-build.bat と同じ方式で、実処理は release-build.ps1(PowerShell)に委ねる。
rem 使い方は README.md「リリースビルド」を参照。
rem pwsh(PowerShell 7)が使える場合はそちらを優先する
rem (Windows PowerShell 5.1 はスクリプト内の日本語の扱いで問題が起きることがあるため)。
rem 例: release-build.bat
rem 例: release-build.bat -VersionCode 2 -VersionName 0.2.0
where pwsh >nul 2>nul
if %errorlevel%==0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0release-build.ps1" %*
) else (
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0release-build.ps1" %*
)
exit /b %errorlevel%
