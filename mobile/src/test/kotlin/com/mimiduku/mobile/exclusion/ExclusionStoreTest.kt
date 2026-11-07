package com.mimiduku.mobile.exclusion

import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExclusionStoreTest {
    private val memory = MemoryStringStore()
    private var salt = "salt-a"
    private val store = ExclusionStore(memory, saltGenerator = { salt })

    @Test
    fun `packages are added once and removed individually`() {
        assertTrue(store.addPackage("com.example.game"))
        assertTrue(store.addPackage("com.example.game"))
        assertTrue(store.addPackage("com.example.news"))

        assertEquals(listOf("com.example.game", "com.example.news"), store.packages())
        store.removePackage("com.example.game")
        assertFalse(store.hasPackage("com.example.game"))
        assertTrue(store.hasPackage("com.example.news"))
    }

    @Test
    fun `blank or malformed package names are rejected`() {
        assertFalse(store.addPackage(" "))
        assertFalse(store.addPackage("a b"))
        assertTrue(store.packages().isEmpty())
    }

    @Test
    fun `domains are matched but never stored in plain text`() {
        assertTrue(store.addDomain("example.com"))

        assertTrue(store.hasDomain("example.com"))
        assertFalse(store.hasDomain("evil.example.com"))
        assertEquals(1, store.domainCount())
        assertTrue(memory.values.values.none { "example" in it })
    }

    @Test
    fun `the same domain is registered once and can be cleared`() {
        store.addDomain("example.com")
        store.addDomain("example.com")
        assertEquals(1, store.domainCount())

        store.clearDomains()
        assertEquals(0, store.domainCount())
        assertFalse(store.hasDomain("example.com"))
    }

    @Test
    fun `the salt is generated once and kept so earlier entries still match`() {
        store.addDomain("example.com")
        salt = "salt-b"

        assertTrue(store.hasDomain("example.com"))
    }

    @Test
    fun `the number of entries is limited`() {
        repeat(ExclusionStore.MAX_ENTRIES) { assertTrue(store.addDomain("site$it.example.com")) }
        assertFalse(store.addDomain("one-more.example.com"))
        repeat(ExclusionStore.MAX_ENTRIES) { assertTrue(store.addPackage("com.example.app$it")) }
        assertFalse(store.addPackage("com.example.extra"))
    }
}
