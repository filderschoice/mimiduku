package com.mimiduku.mobile.mode

import com.mimiduku.mobile.storage.StringStore

/**
 * アクセシビリティ API の利用についての、利用者の同意（目立つ開示と同意。docs/design/DISTRIBUTION.md 2章）。
 *
 * 同意した開示文の版数を保存する。開示文の内容（読み取る情報や用途）を変えたら [CURRENT_VERSION] を上げ、
 * 改めて同意を求める。同意がない間、サービスはイベントを処理しない。
 */
class ConsentStore(
    private val store: StringStore,
) {
    fun hasConsented(): Boolean = store.getString(KEY)?.toIntOrNull() == CURRENT_VERSION

    fun accept() {
        store.putString(KEY, CURRENT_VERSION.toString())
    }

    companion object {
        /** 開示文の版数。strings.xml の disclosure_* を変えたら上げる。 */
        const val CURRENT_VERSION = 1
        private const val KEY = "accessibility_consent_version"
    }
}
