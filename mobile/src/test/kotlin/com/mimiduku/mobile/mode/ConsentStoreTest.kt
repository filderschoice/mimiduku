package com.mimiduku.mobile.mode

import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 開示文の版数と同意（DESIGN.md の FR-20）。サービスは [ConsentStore.hasConsented] が false の間イベントを処理しないため、
 * ここで false になることが「再同意するまで保護が動かない」ことに当たる。
 */
class ConsentStoreTest {
    private val store = MemoryStringStore()
    private val consent = ConsentStore(store)

    @Test
    fun `nothing is consented by default`() {
        assertFalse(consent.hasConsented())
    }

    @Test
    fun `a consent given to the first disclosure version must be given again`() {
        // BL-074 より前の版（1）で同意したまま更新した利用者。
        store.values[KEY] = "1"

        assertFalse(consent.hasConsented())

        consent.accept()

        assertTrue(consent.hasConsented())
        assertEquals(ConsentStore.CURRENT_VERSION.toString(), store.values[KEY])
    }

    @Test
    fun `the disclosure version was raised for the site exclusion disclosure`() {
        assertEquals(2, ConsentStore.CURRENT_VERSION)
    }

    @Test
    fun `an unreadable stored version is not a consent`() {
        store.values[KEY] = "abc"

        assertFalse(consent.hasConsented())
    }

    private companion object {
        const val KEY = "accessibility_consent_version"
    }
}
