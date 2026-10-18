package com.mimiduku.mobile.log

import com.mimiduku.mobile.mode.UsageMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** 検知の種類。 */
enum class DetectionKind {
    EXTERNAL_TRANSITION,
    SCAREWARE,
}

/** 検知したときに行った介入。 */
enum class InterventionAction {
    /** 自動で戻り、警告を出した。 */
    BACK_AND_WARN,

    /** 警告だけを出した。 */
    WARN_ONLY,

    /** 「戻る」「このまま進む」を出して利用者に選ばせた。 */
    ASK_USER,
}

/**
 * 検知ログ1件。
 *
 * プライバシーのため、画面の文字・URL は持たない。残すのは日時・パッケージ名・検知の種類・一致したルールの ID・
 * 介入の内容・直前のクリックの有無・利用モードだけ（MVP_DESIGN.md 10章）。
 */
@Serializable
data class DetectionLogEntry(
    val atMillis: Long,
    val kind: DetectionKind,
    val sourcePackage: String,
    val targetPackage: String? = null,
    val ruleId: String? = null,
    val action: InterventionAction,
    val clickedRecently: Boolean? = null,
    val usageMode: UsageMode? = null,
)

/**
 * 検知ログをアプリ内ストレージのファイルへ保存する。
 *
 * 件数は [maxEntries] までで、超えたら古いものから捨てる。書き込みは一時ファイルへ書いてから置き換えるため、
 * 途中で落ちても元のファイルは壊れない。読めないファイルは空として扱い、次の追記で正しい内容に置き換える。
 */
class DetectionLog(
    private val file: File,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(DetectionLogEntry.serializer())
    private val lock = Any()

    /** 保存されているログ（古い順）。 */
    fun entries(): List<DetectionLogEntry> = synchronized(lock) { read() }

    fun append(entry: DetectionLogEntry) {
        synchronized(lock) {
            write((read() + entry).takeLast(maxEntries))
        }
    }

    fun clear() {
        synchronized(lock) { write(emptyList()) }
    }

    private fun read(): List<DetectionLogEntry> {
        if (!file.exists()) return emptyList()
        return runCatching { json.decodeFromString(serializer, file.readText()) }
            .onFailure { if (it !is IOException && it !is IllegalArgumentException) throw it }
            .getOrDefault(emptyList())
    }

    private fun write(entries: List<DetectionLogEntry>) {
        file.parentFile?.mkdirs()
        val temp = File(file.path + ".tmp")
        temp.writeText(json.encodeToString(serializer, entries))
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 500
        const val FILE_NAME = "detection-log.json"
    }
}
