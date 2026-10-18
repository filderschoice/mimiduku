package com.mimiduku.mobile.settings

/** 設定画面に出す状態の判定。Android 非依存にして単体テストする。 */
object SetupStatus {
    /** Android 13（API 33）から、サイドロードしたアプリのアクセシビリティ有効化に「制限された設定」の解除が要る。 */
    const val RESTRICTED_SETTINGS_SDK = 33

    /**
     * `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` の値に、本アプリのサービスが含まれるか。
     *
     * 値は `パッケージ名/クラス名` を `:` でつないだもの。クラス名はパッケージ名を省いた `.Class` の形でも
     * 書かれうるため、両方の形と比べる。
     */
    fun isServiceEnabled(
        enabledServices: String?,
        packageName: String,
        className: String,
    ): Boolean {
        if (enabledServices.isNullOrBlank()) return false
        val shortClassName = className.removePrefix(packageName).takeIf { it.startsWith(".") }
        val candidates = setOfNotNull("$packageName/$className", shortClassName?.let { "$packageName/$it" })
        return enabledServices.split(':').any { entry -> candidates.any { it.equals(entry.trim(), ignoreCase = true) } }
    }

    fun needsRestrictedSettingsGuide(sdkInt: Int): Boolean = sdkInt >= RESTRICTED_SETTINGS_SDK
}
