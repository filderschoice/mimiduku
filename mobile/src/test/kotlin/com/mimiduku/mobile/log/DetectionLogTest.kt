package com.mimiduku.mobile.log

import com.mimiduku.mobile.mode.UsageMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
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
                "recordedAtServiceEnd",
            )
        assertTrue(keys.isNotEmpty() && allowed.containsAll(keys))
    }

    @Test
    fun `an ordinary entry is stored without the service end mark`() {
        DetectionLog(file).append(entry(1))

        // 既定値は書き出さないため、通常の行の保存形式は BL-086 の前と同じ。
        assertFalse(file.readText().contains("recordedAtServiceEnd"))
    }

    @Test
    fun `an entry recorded at the service end keeps its mark`() {
        val marked = entry(1, action = InterventionAction.ASK_USER).copy(recordedAtServiceEnd = true)

        DetectionLog(file).append(marked)

        assertEquals(listOf(marked), DetectionLog(file).entries())
    }

    @Test
    fun `an old entry without the service end mark reads as an ordinary entry`() {
        // BL-086 より前の版が保存した行（印の項目が無い）。
        file.writeText(
            "[{\"atMillis\":1,\"kind\":\"EXTERNAL_TRANSITION\",\"sourcePackage\":\"com.example.game\"," +
                "\"targetPackage\":\"com.android.chrome\",\"action\":\"ASK_USER\",\"clickedRecently\":false," +
                "\"usageMode\":\"ADULT\"}]",
        )

        val read = DetectionLog(file).entries().single()

        assertEquals(InterventionAction.ASK_USER, read.action)
        assertFalse(read.recordedAtServiceEnd)
    }

    @Test
    fun `an old version reads a log with the service end mark without losing entries`() {
        val log = DetectionLog(file)
        log.append(entry(1))
        log.append(entry(2, action = InterventionAction.ASK_USER).copy(recordedAtServiceEnd = true))

        // 印の項目を知らない古い版の読み方（同じ ignoreUnknownKeys の設定と、印の無い行の型）で読む。
        val old = OLD_VERSION_JSON.decodeFromString(ListSerializer(OldEntry.serializer()), file.readText())

        assertEquals(listOf(1L, 2L), old.map { it.atMillis })
        assertEquals(listOf(InterventionAction.WARN_ONLY, InterventionAction.ASK_USER), old.map { it.action })
    }

    /** BL-086 より前の [DetectionLogEntry]（終了時の印の項目が無い）。 */
    @Serializable
    private data class OldEntry(
        val atMillis: Long,
        val kind: DetectionKind,
        val sourcePackage: String,
        val targetPackage: String? = null,
        val ruleId: String? = null,
        val action: InterventionAction,
        val clickedRecently: Boolean? = null,
        val usageMode: UsageMode? = null,
    )

    private fun entry(
        at: Long,
        ruleId: String? = null,
        action: InterventionAction = InterventionAction.WARN_ONLY,
    ) = DetectionLogEntry(
        atMillis = at,
        kind = if (ruleId == null) DetectionKind.EXTERNAL_TRANSITION else DetectionKind.SCAREWARE,
        sourcePackage = "com.example.game",
        targetPackage = if (ruleId == null) "com.android.chrome" else null,
        ruleId = ruleId,
        action = action,
    )

    private companion object {
        /** 古い版の [DetectionLog] と同じ読み込みの設定。 */
        val OLD_VERSION_JSON = Json { ignoreUnknownKeys = true }
    }
}
