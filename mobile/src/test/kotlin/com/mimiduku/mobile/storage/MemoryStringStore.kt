package com.mimiduku.mobile.storage

/** 単体テスト用のメモリ上の保存先。 */
class MemoryStringStore : StringStore {
    val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
