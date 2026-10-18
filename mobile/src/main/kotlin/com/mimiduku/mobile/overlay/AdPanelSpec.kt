package com.mimiduku.mobile.overlay

import androidx.annotation.StringRes
import com.mimiduku.mobile.R
import com.mimiduku.mobile.detect.AdScreenState
import com.mimiduku.mobile.mode.UsageMode

/** 広告画面の案内パネルに出す内容。 */
data class AdPanelSpec(
    @StringRes val messageRes: Int,
    /** 残り秒数（`%1$d`）を入れる文言。 */
    @StringRes val countdownMessageRes: Int,
    val countdownSeconds: Int?,
    @StringRes val backLabelRes: Int,
) {
    companion object {
        fun from(state: AdScreenState): AdPanelSpec =
            when (state.mode) {
                UsageMode.CHILD ->
                    AdPanelSpec(
                        messageRes = R.string.child_ad_panel_message,
                        countdownMessageRes = R.string.child_ad_panel_countdown,
                        countdownSeconds = state.countdownSeconds,
                        backLabelRes = R.string.child_ad_panel_back,
                    )
                UsageMode.ADULT ->
                    AdPanelSpec(
                        messageRes = R.string.adult_ad_panel_message,
                        countdownMessageRes = R.string.adult_ad_panel_countdown,
                        countdownSeconds = state.countdownSeconds,
                        backLabelRes = R.string.adult_go_back,
                    )
            }
    }
}
