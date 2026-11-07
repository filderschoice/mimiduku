package com.mimiduku.mobile.settings

import androidx.annotation.StringRes
import com.mimiduku.mobile.R
import com.mimiduku.mobile.log.DetectionKind
import com.mimiduku.mobile.log.DetectionLogEntry
import com.mimiduku.mobile.log.InterventionAction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** ログ閲覧画面の1行。 */
data class LogRow(
    val time: String,
    /** 遷移元（スケアウェアでは表示していたアプリ）。アプリ名が分からなければパッケージ名。 */
    val sourceApp: String,
    /** 遷移先。スケアウェアでは null。 */
    val targetApp: String?,
    @StringRes val kindRes: Int,
    @StringRes val actionRes: Int,
)

/** 検知ログを画面に出す行へ変える。新しい順に並べる。 */
object LogRows {
    private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

    fun from(
        entries: List<DetectionLogEntry>,
        zone: ZoneId,
        appLabel: (String) -> String?,
    ): List<LogRow> =
        entries
            .sortedByDescending { it.atMillis }
            .map { entry ->
                LogRow(
                    time = formatter.format(Instant.ofEpochMilli(entry.atMillis).atZone(zone)),
                    sourceApp = appLabel(entry.sourcePackage) ?: entry.sourcePackage,
                    targetApp = entry.targetPackage?.let { appLabel(it) ?: it },
                    kindRes = kindRes(entry.kind),
                    actionRes = actionRes(entry.action),
                )
            }

    @StringRes
    private fun kindRes(kind: DetectionKind) =
        when (kind) {
            DetectionKind.EXTERNAL_TRANSITION -> R.string.log_kind_external_transition
            DetectionKind.SCAREWARE -> R.string.log_kind_scareware
        }

    @StringRes
    private fun actionRes(action: InterventionAction) =
        when (action) {
            InterventionAction.BACK_AND_WARN -> R.string.log_action_back_and_warn
            InterventionAction.WARN_ONLY -> R.string.log_action_warn_only
            InterventionAction.ASK_USER -> R.string.log_action_ask_user
            InterventionAction.SKIPPED_EXCLUDED -> R.string.log_action_skipped_excluded
        }
}
