package com.mimiduku.mobile.settings

import com.mimiduku.mobile.R
import com.mimiduku.mobile.log.DetectionKind
import com.mimiduku.mobile.log.DetectionLogEntry
import com.mimiduku.mobile.log.InterventionAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset

class LogRowsTest {
    @Test
    fun `rows are ordered from the newest entry`() {
        val entries =
            listOf(
                entry(atMillis = 2_000, source = "com.example.second"),
                entry(atMillis = 3_000, source = "com.example.third"),
                entry(atMillis = 1_000, source = "com.example.first"),
            )

        val rows = LogRows.from(entries, ZoneOffset.UTC) { null }

        assertEquals(listOf("com.example.third", "com.example.second", "com.example.first"), rows.map { it.sourceApp })
    }

    @Test
    fun `app labels are shown and the package name is used when no label is found`() {
        val labels = mapOf("com.example.source" to "Source App", "com.example.target" to "Target App")
        val labeledEntry = entry(source = "com.example.source", target = "com.example.target")
        val unlabeledEntry = entry(source = "com.example.unknown", target = "com.example.other")

        val labeled = LogRows.from(listOf(labeledEntry), ZONE) { labels[it] }
        val unlabeled = LogRows.from(listOf(unlabeledEntry), ZONE) { labels[it] }

        assertEquals("Source App", labeled.single().sourceApp)
        assertEquals("Target App", labeled.single().targetApp)
        assertEquals("com.example.unknown", unlabeled.single().sourceApp)
        assertEquals("com.example.other", unlabeled.single().targetApp)
    }

    @Test
    fun `a scareware entry without a target has no target app`() {
        val row =
            LogRows
                .from(listOf(entry(kind = DetectionKind.SCAREWARE, target = null)), ZONE) { "Some App" }
                .single()

        assertNull(row.targetApp)
        assertEquals("Some App", row.sourceApp)
    }

    @Test
    fun `each detection kind maps to its own string`() {
        val expected =
            mapOf(
                DetectionKind.EXTERNAL_TRANSITION to R.string.log_kind_external_transition,
                DetectionKind.SCAREWARE to R.string.log_kind_scareware,
            )

        DetectionKind.entries.forEach { kind ->
            val row = LogRows.from(listOf(entry(kind = kind)), ZONE) { null }.single()
            assertEquals("kind $kind", expected.getValue(kind), row.kindRes)
        }
    }

    @Test
    fun `each intervention action maps to its own string`() {
        val expected =
            mapOf(
                InterventionAction.BACK_AND_WARN to R.string.log_action_back_and_warn,
                InterventionAction.WARN_ONLY to R.string.log_action_warn_only,
                InterventionAction.ASK_USER to R.string.log_action_ask_user,
                InterventionAction.SKIPPED_EXCLUDED to R.string.log_action_skipped_excluded,
            )

        InterventionAction.entries.forEach { action ->
            val row = LogRows.from(listOf(entry(action = action)), ZONE) { null }.single()
            assertEquals("action $action", expected.getValue(action), row.actionRes)
        }
    }

    @Test
    fun `the time is formatted in the given zone`() {
        // 2026-11-13T00:30:05Z
        val atMillis = 1_794_529_805_000L
        val entries = listOf(entry(atMillis = atMillis))

        val utc = LogRows.from(entries, ZoneOffset.UTC) { null }.single()
        val tokyo = LogRows.from(entries, ZONE) { null }.single()

        assertEquals("2026/11/13 00:30:05", utc.time)
        assertEquals("2026/11/13 09:30:05", tokyo.time)
    }

    @Test
    fun `no entries make no rows`() {
        assertEquals(emptyList<LogRow>(), LogRows.from(emptyList(), ZONE) { null })
    }

    private fun entry(
        atMillis: Long = 0,
        kind: DetectionKind = DetectionKind.EXTERNAL_TRANSITION,
        source: String = "com.example.source",
        target: String? = "com.example.target",
        action: InterventionAction = InterventionAction.WARN_ONLY,
    ) = DetectionLogEntry(
        atMillis = atMillis,
        kind = kind,
        sourcePackage = source,
        targetPackage = target,
        action = action,
    )

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Tokyo")
    }
}
