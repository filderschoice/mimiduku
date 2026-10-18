package com.mimiduku.mobile.detect

import com.mimiduku.mobile.rules.DetectionRules

/** 外部遷移先の種類。 */
enum class TransitionTarget {
    BROWSER,
    STORE,
    INSTALLER,
}

/**
 * パッケージ名を、外部遷移の判定に使う種類へ分ける。
 *
 * ルールの一覧に、端末から動的に得た一覧（`VIEW https` を解決するブラウザ、`HOME` を解決するランチャー、
 * 有効なキーボード）を足して使う。端末から得る処理は Android 依存のため呼び出し側で行う。
 */
class PackageClassifier(
    rules: DetectionRules,
    ownPackage: String,
    deviceBrowsers: Set<String> = emptySet(),
    launchers: Set<String> = emptySet(),
    inputMethods: Set<String> = emptySet(),
) {
    private val browsers = rules.browserPackages.toSet() + deviceBrowsers
    private val stores = rules.storePackages.toSet()
    private val installers = rules.installerPackages.toSet()
    private val transient = rules.transientPackages.toSet() + inputMethods + ownPackage
    private val ignoredSources = rules.ignoredSourcePackages.toSet() + launchers

    /** 外部遷移先として扱う種類。対象外なら null。ストアの判定をブラウザより優先する。 */
    fun targetOf(packageName: String): TransitionTarget? =
        when (packageName) {
            in stores -> TransitionTarget.STORE
            in installers -> TransitionTarget.INSTALLER
            in browsers -> TransitionTarget.BROWSER
            else -> null
        }

    /** 前面に一時的に重なるだけのもの。前面アプリの変化として扱わない。 */
    fun isTransient(packageName: String): Boolean = packageName in transient

    /** ここからの遷移は利用者の意図した操作とみなすもの。 */
    fun isIgnoredSource(packageName: String): Boolean = packageName in ignoredSources
}
