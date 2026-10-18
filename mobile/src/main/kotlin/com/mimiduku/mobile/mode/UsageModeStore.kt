package com.mimiduku.mobile.mode

import android.content.SharedPreferences

/** 文字列の保存先。単体テストで Android の SharedPreferences を使わずに済むよう分けている。 */
interface StringStore {
    fun getString(key: String): String?

    fun putString(
        key: String,
        value: String,
    )
}

/** SharedPreferences を [StringStore] として使う。 */
class SharedPreferencesStringStore(
    private val preferences: SharedPreferences,
) : StringStore {
    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(
        key: String,
        value: String,
    ) {
        preferences.edit().putString(key, value).apply()
    }
}

/** 利用モードを保存する。保存値が無い・読めない場合は [UsageMode.DEFAULT]。 */
class UsageModeStore(
    private val store: StringStore,
) {
    fun load(): UsageMode = UsageMode.fromStored(store.getString(KEY))

    fun save(mode: UsageMode) {
        store.putString(KEY, mode.name)
    }

    companion object {
        const val PREFERENCES_NAME = "mimiduku"
        private const val KEY = "usage_mode"
    }
}
