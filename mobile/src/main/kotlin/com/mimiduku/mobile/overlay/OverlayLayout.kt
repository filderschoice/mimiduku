package com.mimiduku.mobile.overlay

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager

/** オーバーレイの各ウィンドウの配置。いずれも TYPE_ACCESSIBILITY_OVERLAY で、フォーカスは取らない。 */
internal object OverlayLayout {
    /**
     * バッジのウィンドウ。ドラッグを受けるためタッチは受け取る（大きさはバッジ1つ分だけで、外側のタッチは下へ届く）。
     * 画面の外へ置いて最小化できるよう、画面の範囲の制限は外す。座標は画面左上が原点。
     */
    fun badge(size: Int): WindowManager.LayoutParams =
        WindowManager
            .LayoutParams(
                size,
                size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).also { it.gravity = Gravity.TOP or Gravity.START }

    fun shield() =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )

    fun edge(gravity: Int) =
        WindowManager
            .LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).also { it.gravity = gravity }
}
