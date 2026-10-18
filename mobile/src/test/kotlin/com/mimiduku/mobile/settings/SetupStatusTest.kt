package com.mimiduku.mobile.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupStatusTest {
    @Test
    fun `the service is enabled when its full name is listed`() {
        val value = "com.example.other/com.example.other.Service:$PACKAGE/$CLASS"

        assertTrue(SetupStatus.isServiceEnabled(value, PACKAGE, CLASS))
    }

    @Test
    fun `the short class name form is recognized`() {
        assertTrue(SetupStatus.isServiceEnabled("$PACKAGE/.service.MimidukuAccessibilityService", PACKAGE, CLASS))
    }

    @Test
    fun `the service is disabled when not listed or the value is empty`() {
        assertFalse(SetupStatus.isServiceEnabled("com.example.other/com.example.other.Service", PACKAGE, CLASS))
        assertFalse(SetupStatus.isServiceEnabled("", PACKAGE, CLASS))
        assertFalse(SetupStatus.isServiceEnabled(null, PACKAGE, CLASS))
    }

    @Test
    fun `a debug build with a different package is not confused with the release build`() {
        val debugPackage = "$PACKAGE.debug"

        assertFalse(SetupStatus.isServiceEnabled("$PACKAGE/$CLASS", debugPackage, CLASS))
        assertTrue(SetupStatus.isServiceEnabled("$debugPackage/$CLASS", debugPackage, CLASS))
    }

    @Test
    fun `the restricted settings guide is shown from android 13`() {
        assertFalse(SetupStatus.needsRestrictedSettingsGuide(32))
        assertTrue(SetupStatus.needsRestrictedSettingsGuide(33))
        assertTrue(SetupStatus.needsRestrictedSettingsGuide(36))
    }

    private companion object {
        const val PACKAGE = "com.mimiduku.mobile"
        const val CLASS = "com.mimiduku.mobile.service.MimidukuAccessibilityService"
    }
}
