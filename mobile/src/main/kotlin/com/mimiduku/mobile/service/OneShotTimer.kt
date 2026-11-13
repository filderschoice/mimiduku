package com.mimiduku.mobile.service

/** 少し待ってから 1 回だけ行う処理の予約。新しく予約すると、前の予約は取り消す（実装は [DelayedAction]）。 */
interface OneShotTimer {
    fun schedule(
        delayMillis: Long,
        action: () -> Unit,
    )

    fun cancel()
}
