package com.mimiduku.mobile.overlay

import androidx.annotation.StringRes
import com.mimiduku.mobile.R
import com.mimiduku.mobile.detect.TransitionTarget
import com.mimiduku.mobile.log.InterventionAction
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.service.Detection

/** バナーのボタンを押したときの動作。 */
enum class BannerAction {
    DISMISS,
    GO_BACK,
}

/** バナーに出す内容。文言は文字列リソースの ID で持ち、表示時に解決する。 */
data class BannerSpec(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    /** タイトル・本文の `%1$s` に入れる文字列リソース（遷移先の種類）。不要なら null。 */
    @StringRes val formatArgRes: Int?,
    @StringRes val primaryLabelRes: Int,
    val primaryAction: BannerAction,
    @StringRes val secondaryLabelRes: Int?,
    val autoDismissMillis: Long,
) {
    companion object {
        const val WARNING_DISMISS_MILLIS = 8_000L
        const val ASK_DISMISS_MILLIS = 20_000L

        fun from(detection: Detection): BannerSpec {
            val target = detection.transition?.target
            return if (target == null) scareware(detection.intervention.mode) else transition(detection, target)
        }

        private fun scareware(mode: UsageMode) =
            when (mode) {
                UsageMode.CHILD -> warning(R.string.child_scareware_title, R.string.child_scareware_message)
                UsageMode.ADULT ->
                    warning(
                        R.string.adult_scareware_title,
                        R.string.adult_scareware_message,
                        close = R.string.adult_close,
                    )
            }

        private fun transition(
            detection: Detection,
            target: TransitionTarget,
        ): BannerSpec {
            val targetRes = targetLabelRes(target)
            return when (detection.intervention.action) {
                InterventionAction.BACK_AND_WARN ->
                    warning(
                        R.string.child_transition_blocked_title,
                        R.string.child_transition_blocked_message,
                        null,
                        R.string.child_close,
                    )
                InterventionAction.WARN_ONLY ->
                    if (detection.intervention.mode == UsageMode.CHILD) {
                        warning(
                            R.string.child_transition_warn_title,
                            R.string.child_transition_warn_message,
                            null,
                            R.string.child_close,
                        )
                    } else {
                        warning(
                            R.string.adult_transition_title,
                            R.string.adult_transition_warn_message,
                            targetRes,
                            R.string.adult_close,
                        )
                    }
                InterventionAction.ASK_USER ->
                    BannerSpec(
                        titleRes = R.string.adult_transition_title,
                        messageRes = R.string.adult_transition_message,
                        formatArgRes = targetRes,
                        primaryLabelRes = R.string.adult_go_back,
                        primaryAction = BannerAction.GO_BACK,
                        secondaryLabelRes = R.string.adult_proceed,
                        autoDismissMillis = ASK_DISMISS_MILLIS,
                    )
            }
        }

        private fun warning(
            @StringRes title: Int,
            @StringRes message: Int,
            @StringRes formatArg: Int? = null,
            @StringRes close: Int = R.string.child_close,
        ) = BannerSpec(title, message, formatArg, close, BannerAction.DISMISS, null, WARNING_DISMISS_MILLIS)

        @StringRes
        fun targetLabelRes(target: TransitionTarget): Int =
            when (target) {
                TransitionTarget.BROWSER -> R.string.target_browser
                TransitionTarget.STORE -> R.string.target_store
                TransitionTarget.INSTALLER -> R.string.target_installer
            }
    }
}
