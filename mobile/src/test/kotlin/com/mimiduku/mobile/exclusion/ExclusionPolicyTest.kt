package com.mimiduku.mobile.exclusion

import com.mimiduku.mobile.detect.ExternalTransition
import com.mimiduku.mobile.detect.TransitionTarget
import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExclusionPolicyTest {
    private val memory = MemoryStringStore()
    private val store =
        ExclusionStore(
            memory,
            saltGenerator = { "salt" },
            secrets = DomainSecrets(memory, DomainVault(iterations = 1_000)),
        )

    private fun transition(target: TransitionTarget) =
        ExternalTransition("com.example.game", "target.pkg", target, clickedRecently = true, repeatCount = 1)

    @Test
    fun `a registered source app skips browser and store transitions but not installers`() {
        store.addPackage("com.example.game")

        assertTrue(ExclusionPolicy.skipsByPackage(store, transition(TransitionTarget.BROWSER)))
        assertTrue(ExclusionPolicy.skipsByPackage(store, transition(TransitionTarget.STORE)))
        assertFalse(ExclusionPolicy.skipsByPackage(store, transition(TransitionTarget.INSTALLER)))
    }

    @Test
    fun `an unregistered source app is not skipped`() {
        store.addPackage("com.example.other")

        assertFalse(ExclusionPolicy.skipsByPackage(store, transition(TransitionTarget.BROWSER)))
    }

    @Test
    fun `domains are checked only for browser transitions when sites are registered`() {
        assertFalse(ExclusionPolicy.needsDomain(store, transition(TransitionTarget.BROWSER)))

        store.addDomain("example.com", "1234")

        assertTrue(ExclusionPolicy.needsDomain(store, transition(TransitionTarget.BROWSER)))
        assertFalse(ExclusionPolicy.needsDomain(store, transition(TransitionTarget.STORE)))
        assertFalse(ExclusionPolicy.needsDomain(store, transition(TransitionTarget.INSTALLER)))
    }

    @Test
    fun `an unreadable domain never skips the confirmation`() {
        store.addDomain("example.com", "1234")

        assertTrue(ExclusionPolicy.skipsByDomain(store, "example.com"))
        assertFalse(ExclusionPolicy.skipsByDomain(store, "other.com"))
        assertFalse(ExclusionPolicy.skipsByDomain(store, null))
    }
}
