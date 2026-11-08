package com.mimiduku.mobile.exclusion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DomainNormalizerTest {
    @Test
    fun `host is extracted from urls with and without a scheme`() {
        assertEquals("example.com", DomainNormalizer.fromUrlText("https://example.com/path?q=1#top"))
        assertEquals("example.com", DomainNormalizer.fromUrlText("example.com/path"))
        assertEquals("example.com", DomainNormalizer.fromUrlText("  EXAMPLE.com  "))
    }

    @Test
    fun `www port and trailing dot are dropped but other subdomains are kept`() {
        assertEquals("example.com", DomainNormalizer.fromUrlText("https://www.example.com:8443/"))
        assertEquals("example.com", DomainNormalizer.fromUrlText("example.com."))
        assertEquals("m.example.com", DomainNormalizer.fromUrlText("https://m.example.com"))
    }

    @Test
    fun `the host after at sign is used so a spoofed prefix does not match`() {
        assertEquals("evil.com", DomainNormalizer.fromUrlText("https://good.com@evil.com/"))
    }

    @Test
    fun `text that is not a host is rejected`() {
        assertNull(DomainNormalizer.fromUrlText(null))
        assertNull(DomainNormalizer.fromUrlText(""))
        assertNull(DomainNormalizer.fromUrlText("みみづく 広告"))
        assertNull(DomainNormalizer.fromUrlText("how to cook rice"))
        assertNull(DomainNormalizer.fromUrlText("localhost"))
        assertNull(DomainNormalizer.fromUrlText("https://"))
        assertNull(DomainNormalizer.fromUrlText("-bad-.com"))
    }
}
