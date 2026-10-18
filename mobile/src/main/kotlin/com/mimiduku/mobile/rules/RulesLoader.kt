package com.mimiduku.mobile.rules

import kotlinx.serialization.json.Json

/** 検知ルールの JSON を読み込む。読めない・形式が合わない場合は既定のルールへ戻す。 */
object RulesLoader {
    private val json = Json { ignoreUnknownKeys = true }

    /** 読み込みの結果。[usedFallback] が真のとき、[rules] は既定のルール。 */
    data class Result(
        val rules: DetectionRules,
        val usedFallback: Boolean,
    )

    fun parse(text: String?): Result {
        val rules = text?.takeIf { it.isNotBlank() }?.let(::decodeOrNull)?.takeIf(::isValid)
        return rules?.let { Result(it, usedFallback = false) }
            ?: Result(DetectionRules.DEFAULT, usedFallback = true)
    }

    /** 壊れた JSON・必須の項目の欠けは null にする（SerializationException は IllegalArgumentException の派生）。 */
    private fun decodeOrNull(text: String): DetectionRules? =
        runCatching { json.decodeFromString(DetectionRules.serializer(), text) }
            .onFailure { if (it !is IllegalArgumentException) throw it }
            .getOrNull()

    private fun isValid(rules: DetectionRules): Boolean =
        rules.version == DetectionRules.SUPPORTED_VERSION &&
            rules.browserPackages.isNotEmpty() &&
            rules.scarewarePhrases.isNotEmpty() &&
            rules.scarewarePhrases.all { it.id.isNotBlank() && it.text.isNotBlank() }
}
