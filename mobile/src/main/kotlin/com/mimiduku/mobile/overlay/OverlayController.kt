package com.mimiduku.mobile.overlay

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.mimiduku.mobile.R

/**
 * 警告バナーを `TYPE_ACCESSIBILITY_OVERLAY` で画面上部に出す。
 *
 * バナーは `FLAG_NOT_TOUCH_MODAL` で自分の領域のタッチだけを受け取り、バナーの外のタッチは下のアプリへ届く
 * （タップを吸収するシールドは MVP の範囲外）。同時に出すバナーは1つで、新しいものが来たら置き換える。
 * AccessibilityService のコンテキストから使うこと（他のコンテキストではこの種類のウィンドウを追加できない）。
 */
class OverlayController(
    private val context: Context,
    /** バッジの置き場所を保存する。ドラッグや最小化のたびに呼ばれる。 */
    private val onBadgePlacementChanged: (BadgePlacement) -> Unit,
    private val onGoBack: () -> Unit,
    /** バッジの「×」が押されたとき。バッジを隠す設定へ切り替える。 */
    private val onBadgeClose: () -> Unit = {},
    /** 案内パネルの「もどる」。本物の閉じるボタンの代理タップを試し、だめなら戻る（BL-021）。 */
    private val onAdClose: () -> Unit = onGoBack,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable { dismiss() }
    private var current: View? = null
    private var adPanel: View? = null
    private var adPanelSpec: AdPanelSpec? = null
    private var shield: View? = null
    private val badge = BadgeOverlay(context, windowManager, onBadgePlacementChanged, onBadgeClose)
    private val shieldTimeoutRunnable = Runnable { setShield(false) }

    fun show(spec: BannerSpec) {
        dismiss()
        val view = LayoutInflater.from(context).inflate(R.layout.overlay_banner, null)
        val arg = spec.formatArgRes?.let(context::getString)
        view.findViewById<TextView>(R.id.banner_title).text = format(spec.titleRes, arg)
        view.findViewById<TextView>(R.id.banner_message).text = format(spec.messageRes, arg)
        view.findViewById<Button>(R.id.banner_primary).apply {
            setText(spec.primaryLabelRes)
            setOnClickListener {
                dismiss()
                if (spec.primaryAction == BannerAction.GO_BACK) onGoBack()
            }
        }
        spec.secondaryLabelRes?.let { label ->
            view.findViewById<Button>(R.id.banner_secondary).apply {
                visibility = View.VISIBLE
                setText(label)
                setOnClickListener { dismiss() }
            }
        }
        windowManager.addView(view, OverlayLayout.edge(Gravity.TOP))
        current = view
        handler.postDelayed(dismissRunnable, spec.autoDismissMillis)
    }

    /**
     * 広告画面の案内パネルを画面下部に出す（[spec] が null なら消す）。内容が変わらなければ何もしない。
     * 警告バナーとは別のウィンドウで、広告画面にいる間は出し続ける。
     */
    fun showAdPanel(spec: AdPanelSpec?) {
        if (spec == adPanelSpec) return
        adPanelSpec = spec
        if (spec == null) {
            removeAdPanel()
            return
        }
        val view =
            adPanel ?: LayoutInflater.from(context).inflate(R.layout.overlay_ad_panel, null).also { panel ->
                panel.findViewById<Button>(R.id.ad_panel_back).setOnClickListener { onAdClose() }
                windowManager.addView(panel, OverlayLayout.edge(Gravity.BOTTOM))
                adPanel = panel
            }
        view.findViewById<TextView>(R.id.ad_panel_message).text =
            spec.countdownSeconds?.let { context.getString(spec.countdownMessageRes, it) }
                ?: context.getString(spec.messageRes)
        view.findViewById<Button>(R.id.ad_panel_back).setText(spec.backLabelRes)
    }

    /**
     * 全画面のタップシールドを張る・外す。シールドは半透明でタップを吸収し、案内パネル（大きな「もどる」）だけを押せる
     * ようにする。判定の誤りで操作できなくなるのを防ぐため、[ShieldTimer.MAX_SHIELD_MILLIS] で自動的に外す。
     */
    fun setShield(on: Boolean) {
        if (on == (shield != null)) return
        handler.removeCallbacks(shieldTimeoutRunnable)
        if (!on) {
            shield?.let { runCatching { windowManager.removeView(it) } }
            shield = null
            return
        }
        val view =
            View(context).apply {
                setBackgroundColor(SHIELD_COLOR)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                // タップを吸収するだけで、下の広告へは渡さない。
                setOnTouchListener { touched, _ -> touched.performClick() }
                setOnClickListener { }
            }
        windowManager.addView(view, OverlayLayout.shield())
        shield = view
        handler.postDelayed(shieldTimeoutRunnable, ShieldTimer.MAX_SHIELD_MILLIS)
        // 案内パネルをシールドより上に置くため、出ていれば付け直す。
        adPanel?.let { panel ->
            runCatching { windowManager.removeView(panel) }
            windowManager.addView(panel, OverlayLayout.edge(Gravity.BOTTOM))
        }
    }

    /**
     * みみづくバッジを出す（[expression] が null なら消す）。[placement] が置き場所で、バッジは指で動かせる。
     */
    fun setBadge(
        expression: BadgeExpression?,
        placement: BadgePlacement = BadgePlacement.DEFAULT,
    ) = badge.update(expression, placement)

    /** 画面の向きが変わったとき、バッジを同じ置き場所へ合わせ直す。 */
    fun relayoutBadge() = badge.relayout()

    /** 警告バナー・広告画面の案内パネル・シールド・バッジをすべて消す。 */
    fun dismissAll() {
        dismiss()
        adPanelSpec = null
        removeAdPanel()
        setShield(false)
        badge.remove()
    }

    private fun removeAdPanel() {
        adPanel?.let { view ->
            runCatching { windowManager.removeView(view) }
            adPanel = null
        }
    }

    fun dismiss() {
        handler.removeCallbacks(dismissRunnable)
        current?.let { view ->
            runCatching { windowManager.removeView(view) }
            current = null
        }
    }

    private fun format(
        res: Int,
        arg: String?,
    ): String = if (arg == null) context.getString(res) else context.getString(res, arg)

    private companion object {
        /** 広告が透けて見える程度の半透明の黒。 */
        const val SHIELD_COLOR = 0x66000000
    }
}
