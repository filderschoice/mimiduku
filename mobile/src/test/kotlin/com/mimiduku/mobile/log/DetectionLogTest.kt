package com.mimiduku.mobile.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DetectionLogTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file: File get() = File(folder.root, DetectionLog.FILE_NAME)

    @Test
    fun `appended entries are kept in order`() {
        val log = DetectionLog(file)

        log.append(entry(1))
        log.append(entry(2))

        assertEquals(listOf(1L, 2L), log.entries().map { it.atMillis })
    }

    @Test
    fun `entries survive a new instance over the same file`() {
        DetectionLog(file).append(entry(1, ruleId = "virus-infected-ja"))

        assertEquals(listOf(entry(1, ruleId = "virus-infected-ja")), DetectionLog(file).entries())
    }

    @Test
    fun `the oldest entries are dropped beyond the limit`() {
        val log = DetectionLog(file, maxEntries = 3)

        (1L..5L).forEach { log.append(entry(it)) }

        assertEquals(listOf(3L, 4L, 5L), log.entries().map { it.atMillis })
    }

    @Test
    fun `a missing file reads as empty`() {
        assertTrue(DetectionLog(file).entries().isEmpty())
    }

    @Test
    fun `a corrupted file reads as empty and is replaced on the next append`() {
        file.writeText("[{\"atMillis\": 1, \"kind\": ")
        val log = DetectionLog(file)

        assertTrue(log.entries().isEmpty())
        log.append(entry(2))

        assertEquals(listOf(2L), DetectionLog(file).entries().map { it.atMillis })
    }

    @Test
    fun `no temporary file is left after writing`() {
        DetectionLog(file).append(entry(1))

        assertFalse(File(file.path + ".tmp").exists())
    }

    @Test
    fun `clear removes all entries`() {
        val log = DetectionLog(file)
        log.append(entry(1))

        log.clear()

        assertTrue(log.entries().isEmpty())
    }

    @Test
    fun `the stored file holds no screen text`() {
        DetectionLog(file).append(entry(1, ruleId = "virus-infected-ja"))

        val keys = Regex("\"(\\w+)\":").findAll(file.readText()).map { it.groupValues[1] }.toSet()
        val allowed =
            setOf(
                "atMillis",
                "kind",
                "sourcePackage",
                "targetPackage",
                "ruleId",
                "action",
                "clickedRecently",
                "usageMode",
            )
        assertTrue(keys.isNotEmpty() && allowed.containsAll(keys))
    }

    private fun entry(
        at: Long,
        ruleId: String? = null,
    ) = DetectionLogEntry(
        atMillis = at,
        kind = if (ruleId == null) DetectionKind.EXTERNAL_TRANSITION else DetectionKind.SCAREWARE,
        sourcePackage = "com.example.game",
        targetPackage = if (ruleId == null) "com.android.chrome" else null,
        ruleId = ruleId,
        action = InterventionAction.WARN_ONLY,
    )
}
