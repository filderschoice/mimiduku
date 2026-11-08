package com.mimiduku.mobile.exclusion

import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExclusionStoreTest {
    private val memory = MemoryStringStore()
    private var salt = "salt-a"
    private val vault = DomainVault(iterations = 1_000)
    private val store = ExclusionStore(memory, saltGenerator = { salt }, secrets = DomainSecrets(memory, vault))

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
        assertTrue(store.addDomain("example.com", "1234"))

        assertTrue(store.hasDomain("example.com"))
        assertFalse(store.hasDomain("evil.example.com"))
        assertEquals(1, store.domainCount())
        assertTrue(memory.values.values.none { "example" in it })
    }

    @Test
    fun `the same domain is registered once and can be cleared`() {
        store.addDomain("example.com", "1234")
        store.addDomain("example.com", "1234")
        assertEquals(1, store.domainCount())

        store.clearDomains()
        assertEquals(0, store.domainCount())
        assertFalse(store.hasDomain("example.com"))
    }

    @Test
    fun `the salt is generated once and kept so earlier entries still match`() {
        store.addDomain("example.com", "1234")
        salt = "salt-b"

        assertTrue(store.hasDomain("example.com"))
    }

    @Test
    fun `the number of entries is limited`() {
        repeat(ExclusionStore.MAX_ENTRIES) { assertTrue(store.addDomain("site$it.example.com", "1234")) }
        assertFalse(store.addDomain("one-more.example.com", "1234"))
        repeat(ExclusionStore.MAX_ENTRIES) { assertTrue(store.addPackage("com.example.app$it")) }
        assertFalse(store.addPackage("com.example.extra"))
    }

    @Test
    fun `the pin lists the registered sites and a wrong pin shows nothing`() {
        store.addDomain("example.com", "1234")
        store.addDomain("kids.example.org", "1234")

        assertEquals(
            listOf("example.com", "kids.example.org"),
            store.secrets.all("1234").map { it.domain }.sorted(),
        )
        assertTrue(store.secrets.all("9999").isEmpty())
        assertEquals(2, store.secrets.ids().size)
    }

    @Test
    fun `a listed site can be removed individually and the match goes with it`() {
        store.addDomain("example.com", "1234")
        store.addDomain("kids.example.org", "1234")
        val target = store.secrets.all("1234").first { it.domain == "example.com" }

        store.removeDomain(target.id)

        assertFalse(store.hasDomain("example.com"))
        assertTrue(store.hasDomain("kids.example.org"))
        assertEquals(listOf("kids.example.org"), store.secrets.all("1234").map { it.domain })
    }

    @Test
    fun `changing the pin keeps the list readable only with the new pin`() {
        store.addDomain("example.com", "1234")

        store.secrets.reencrypt("1234", "5678")

        assertTrue(store.secrets.all("1234").isEmpty())
        assertEquals(listOf("example.com"), store.secrets.all("5678").map { it.domain })
        assertTrue(store.hasDomain("example.com"))
    }

    @Test
    fun `clearing the domains also drops the encrypted list`() {
        store.addDomain("example.com", "1234")

        store.clearDomains()

        assertEquals(0, store.domainCount())
        assertEquals(0, store.secrets.ids().size)
        assertTrue(memory.values.keys.none { "secrets" in it })
    }

    @Test
    fun `a site registered before the encrypted list existed is counted as unlisted`() {
        memory.values["exclusion_salt"] = "salt-a"
        val legacy = ExclusionStore(MemoryStringStore(), saltGenerator = { "salt-a" })
        legacy.addDomain("old.example.com", "1234")
        store.addDomain("example.com", "1234")
        memory.values["exclusion_domain_secrets"] = ""

        assertEquals(1, store.domainCount())
        assertEquals(1, store.domainCount() - store.secrets.ids().size)
        assertEquals(0, store.secrets.ids().size)
    }

    @Test
    fun `a tampered token does not decrypt`() {
        val key = vault.key("1234", vault.newSalt())
        val token = vault.encrypt("example.com", key)
        val broken = token.dropLast(4) + "AAAA"

        assertEquals("example.com", vault.decrypt(token, key))
        assertEquals(null, vault.decrypt(broken, key))
        assertEquals(null, vault.decrypt("not base64!", key))
    }
}
