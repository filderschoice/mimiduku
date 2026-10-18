package com.mimiduku.mobile.storage

import android.content.Context
import android.content.SharedPreferences

/** 文字列の保存先。単体テストで Android の SharedPreferences を使わずに済むよう分けている。 */
interface StringStore {
    fun getString(key: String): String?

    fun putString(
        key: String,
        value: String,
    )

    fun remove(key: String)
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

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "mimiduku"

        /** アプリ内で共有する保存先（他アプリからは読めない MODE_PRIVATE）。 */
        fun of(context: Context) =
            SharedPreferencesStringStore(context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE))
    }
}
