package com.mimiduku.mobile.service

import android.os.Handler
import android.os.Looper

/** 少し待ってから 1 回だけ行う処理。新しく予約すると、前の予約は取り消す。 */
class DelayedAction(
    private val handler: Handler = Handler(Looper.getMainLooper()),
) : OneShotTimer {
    private var pending: Runnable? = null

    override fun schedule(
        delayMillis: Long,
        action: () -> Unit,
    ) {
        cancel()
        val task =
            Runnable {
                pending = null
                action()
            }
        pending = task
        handler.postDelayed(task, delayMillis)
    }

    override fun cancel() {
        pending?.let(handler::removeCallbacks)
        pending = null
    }
}
