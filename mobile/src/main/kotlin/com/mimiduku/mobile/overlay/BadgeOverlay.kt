package com.mimiduku.mobile.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.mimiduku.mobile.R

/**
 * みみづくバッジのウィンドウ。指でドラッグして動かせ、離すと左右どちらかの端へ吸着する。画面の外へ引き出して離すと
 * その端へ最小化し（一部だけ見える）、最小化中に触ると元へ戻る。最小化中も上下・内側へドラッグできる。
 * 置き場所は [onPlacementChanged] で保存してもらう。AccessibilityService のコンテキストから使うこと。
 * 最小化していないバッジをタップすると隣に「×」が出て、押すと [onClose] でバッジを閉じる（しばらくして自動で消える）。
 */
internal class BadgeOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onPlacementChanged: (BadgePlacement) -> Unit,
    private val onClose: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val hideCloseRunnable = Runnable { hideClose() }
    private var closeView: TextView? = null

    private val density = context.resources.displayMetrics.density
    private val size = (BADGE_SIZE_DP * density).toInt()
    private val closeSize = (CLOSE_SIZE_DP * density).toInt()
    private val peek = (PEEK_DP * density).toInt()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var view: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private var placement = BadgePlacement.DEFAULT

    /** ドラッグ中。この間は、イベントごとの表示更新で置き場所を巻き戻さない。 */
    private var dragging = false

    /** 表示を更新する。[expression] が null なら消す。[newPlacement] が変わっていれば動かす。 */
    fun update(
        expression: BadgeExpression?,
        newPlacement: BadgePlacement,
    ) {
        if (expression == null) {
            remove()
            return
        }
        if (!dragging) placement = newPlacement
        val image = view ?: create()
        image.setImageResource(expression.drawableRes)
        applyPlacement()
    }

    /** 画面の向きや大きさが変わったとき、同じ置き場所の座標を計算し直す。 */
    fun relayout() = applyPlacement()

    fun remove() {
        hideClose()
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        params = null
    }

    private fun create(): ImageView {
        val image =
            ImageView(context).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setOnTouchListener(DragListener())
            }
        val layoutParams = OverlayLayout.badge(size)
        windowManager.addView(image, layoutParams)
        view = image
        params = layoutParams
        return image
    }

    /** 置き場所と今の画面の大きさから座標と濃さを決める（縦向き・横向きのどちらでも端に合う）。 */
    private fun applyPlacement() {
        val image = view
        val layoutParams = params
        if (image == null || layoutParams == null) return
        image.alpha = if (placement.minimized) MINIMIZED_ALPHA else BADGE_ALPHA
        val metrics = context.resources.displayMetrics
        val x = placement.windowX(metrics.widthPixels, size, peek)
        val y = placement.windowY(metrics.heightPixels, size)
        if (layoutParams.x != x || layoutParams.y != y) {
            layoutParams.x = x
            layoutParams.y = y
            runCatching { windowManager.updateViewLayout(image, layoutParams) }
        }
    }

    /** バッジの隣（画面の中央に近い側）に「×」を出す。 */
    private fun showClose(badgeParams: WindowManager.LayoutParams) {
        hideClose()
        val metrics = context.resources.displayMetrics
        val button =
            TextView(context).apply {
                text = "×"
                textSize = CLOSE_TEXT_SP
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                contentDescription = context.getString(R.string.badge_close_description)
                background =
                    GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(CLOSE_BACKGROUND)
                    }
                setOnClickListener {
                    hideClose()
                    onClose()
                }
            }
        val closeParams = OverlayLayout.badge(closeSize)
        val onRightHalf = badgeParams.x + size / 2 > metrics.widthPixels / 2
        closeParams.x = if (onRightHalf) badgeParams.x - closeSize else badgeParams.x + size
        closeParams.y = badgeParams.y + (size - closeSize) / 2
        windowManager.addView(button, closeParams)
        closeView = button
        handler.postDelayed(hideCloseRunnable, CLOSE_TIMEOUT_MILLIS)
    }

    private fun hideClose() {
        handler.removeCallbacks(hideCloseRunnable)
        closeView?.let { runCatching { windowManager.removeView(it) } }
        closeView = null
    }

    private fun release(layoutParams: WindowManager.LayoutParams) {
        dragging = false
        hideClose()
        val metrics = context.resources.displayMetrics
        placement =
            BadgePlacement.fromRelease(
                layoutParams.x,
                layoutParams.y,
                metrics.widthPixels,
                metrics.heightPixels,
                size,
            )
        // ドラッグ中の座標から端の座標へ合わせ直す。
        layoutParams.x = Int.MIN_VALUE
        applyPlacement()
        onPlacementChanged(placement)
    }

    private fun restore() {
        placement = placement.copy(minimized = false)
        applyPlacement()
        onPlacementChanged(placement)
    }

    /** ドラッグと、最小化中のタップでの復帰を扱う。 */
    private inner class DragListener : View.OnTouchListener {
        private var downRawX = 0f
        private var downRawY = 0f
        private var startX = 0
        private var startY = 0

        override fun onTouch(
            touched: View,
            event: MotionEvent,
        ): Boolean {
            val layoutParams = params ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = layoutParams.x
                    startY = layoutParams.y
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> move(layoutParams, event)
                MotionEvent.ACTION_UP -> finish(touched, layoutParams)
                MotionEvent.ACTION_CANCEL -> if (dragging) release(layoutParams)
            }
            return true
        }

        private fun move(
            layoutParams: WindowManager.LayoutParams,
            event: MotionEvent,
        ) {
            val dx = event.rawX - downRawX
            val dy = event.rawY - downRawY
            if (!dragging && dx * dx + dy * dy < touchSlop * touchSlop) return
            dragging = true
            hideClose()
            layoutParams.x = startX + dx.toInt()
            layoutParams.y = startY + dy.toInt()
            view?.let { runCatching { windowManager.updateViewLayout(it, layoutParams) } }
        }

        private fun finish(
            touched: View,
            layoutParams: WindowManager.LayoutParams,
        ) {
            when {
                dragging -> release(layoutParams)
                placement.minimized -> restore()
                closeView != null -> hideClose()
                else -> showClose(layoutParams)
            }
            if (!dragging) touched.performClick()
        }
    }

    private companion object {
        /** バッジの大きさ。ドラッグでつかみやすいよう、操作の目安（48dp）より大きくする。 */
        const val BADGE_SIZE_DP = 64

        /** 最小化したときに画面の中へ残す幅。つかんで引き出せる太さにする。 */
        const val PEEK_DP = 36

        /** 閉じる「×」ボタンの大きさと表示時間。 */
        const val CLOSE_SIZE_DP = 48
        const val CLOSE_TEXT_SP = 24f
        const val CLOSE_TIMEOUT_MILLIS = 4000L
        const val CLOSE_BACKGROUND = 0xE61E3A5F.toInt()

        /** 止まり木にいる小さなバッジが画面の邪魔にならない濃さ。 */
        const val BADGE_ALPHA = 0.85f

        /** 最小化中はさらに目立たなくする。 */
        const val MINIMIZED_ALPHA = 0.6f
    }
}
