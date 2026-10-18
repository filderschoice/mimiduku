package com.mimiduku.mobile.mode

import com.mimiduku.mobile.storage.StringStore

/** 利用モードを保存する。保存値が無い・読めない場合は [UsageMode.DEFAULT]。 */
class UsageModeStore(
    private val store: StringStore,
) {
    fun load(): UsageMode = UsageMode.fromStored(store.getString(KEY))

    fun save(mode: UsageMode) {
        store.putString(KEY, mode.name)
    }

    private companion object {
        const val KEY = "usage_mode"
    }
}
