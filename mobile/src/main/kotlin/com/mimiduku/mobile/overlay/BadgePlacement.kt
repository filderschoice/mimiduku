package com.mimiduku.mobile.overlay

/** バッジが止まる画面の端。 */
enum class BadgeSide { LEFT, RIGHT }

/** バッジの置き場所の計算に使う画面とバッジの寸法（ピクセル）。[peek] は最小化中に画面内へ残す幅。 */
data class BadgeMetrics(
    val screenWidth: Int,
    val screenHeight: Int,
    val badgeSize: Int,
    val peek: Int = 0,
)

/**
 * バッジの置き場所。左右どちらかの端に吸着し、縦位置は画面の高さに対する割合で持つ（縦横の回転や解像度が変わっても
 * 同じ見た目の位置に戻せる）。[minimized] のときは端から一部（のぞき分）だけを見せ、触ると元に戻る。
 * Android に依存しない純粋な計算だけを持つ。
 */
data class BadgePlacement(
    val side: BadgeSide = BadgeSide.RIGHT,
    /** 0 が上端、1 が下端。バッジ全体が画面に収まる範囲での割合。 */
    val verticalFraction: Float = 0.5f,
    val minimized: Boolean = false,
) {
    /** ウィンドウの左端の座標（画面左端が 0）。最小化中は端から [peek] だけを画面内に残す。 */
    fun windowX(
        screenWidth: Int,
        badgeSize: Int,
        peek: Int,
    ): Int =
        when {
            side == BadgeSide.LEFT && minimized -> peek - badgeSize
            side == BadgeSide.LEFT -> 0
            minimized -> screenWidth - peek
            else -> screenWidth - badgeSize
        }

    /** ウィンドウの上端の座標（画面上端が 0）。 */
    fun windowY(
        screenHeight: Int,
        badgeSize: Int,
    ): Int = (verticalFraction * (screenHeight - badgeSize).coerceAtLeast(0)).toInt()

    fun encode(): String = "${side.name},$verticalFraction,$minimized"

    companion object {
        val DEFAULT = BadgePlacement()

        /** 保存した文字列を読み戻す。壊れていれば既定の置き場所にする。 */
        fun decode(value: String?): BadgePlacement {
            val parts = value?.split(',').orEmpty()
            val side = parts.getOrNull(0)?.let { name -> BadgeSide.entries.firstOrNull { it.name == name } }
            val fraction = parts.getOrNull(1)?.toFloatOrNull()?.takeIf { it in 0f..1f }
            val minimized = parts.getOrNull(2)?.toBooleanStrictOrNull()
            return if (side != null && fraction != null && minimized != null) {
                BadgePlacement(side, fraction, minimized)
            } else {
                DEFAULT
            }
        }

        /**
         * ドラッグを離した位置（ウィンドウの左上の座標）から置き場所を決める。バッジの中心が画面の端まで（端の座標を含む）出ていれば
         * その端へ最小化し、そうでなければ近い方の端へ吸着する。最小化したバッジを上下に動かしただけなら最小化のまま。
         *
         * 最小化中のバッジの中心は、のぞき分 [peek] が大きさの半分以上のとき画面の中（端の座標そのもの以上）にある。
         * そのため中心が外へ出ているかだけでは「最小化のまま」を表せない。[wasMinimized] のときは、最小化中の静止位置から
         * 内側へ [badgeSize] の4分の1までのずれ（指のぶれ）なら最小化を保ち、それより内側へ引き出したときだけ通常の表示へ戻す。
         */
        fun fromRelease(
            windowX: Int,
            windowY: Int,
            screenWidth: Int,
            screenHeight: Int,
            badgeSize: Int,
        ): BadgePlacement =
            fromRelease(windowX, windowY, BadgeMetrics(screenWidth, screenHeight, badgeSize), wasMinimized = false)

        fun fromRelease(
            windowX: Int,
            windowY: Int,
            metrics: BadgeMetrics,
            wasMinimized: Boolean,
        ): BadgePlacement {
            val badgeSize = metrics.badgeSize
            val screenWidth = metrics.screenWidth
            val peek = metrics.peek
            val centerX = windowX + badgeSize / 2
            val range = (metrics.screenHeight - badgeSize).coerceAtLeast(1)
            val fraction = (windowY.toFloat() / range).coerceIn(0f, 1f)
            val restOffset = peek - badgeSize / 2
            val tolerance = badgeSize / 4
            return when {
                centerX <= 0 -> BadgePlacement(BadgeSide.LEFT, fraction, minimized = true)
                centerX >= screenWidth -> BadgePlacement(BadgeSide.RIGHT, fraction, minimized = true)
                wasMinimized && peek > 0 && centerX <= restOffset + tolerance ->
                    BadgePlacement(BadgeSide.LEFT, fraction, minimized = true)
                wasMinimized && peek > 0 && centerX >= screenWidth - restOffset - tolerance ->
                    BadgePlacement(BadgeSide.RIGHT, fraction, minimized = true)
                centerX < screenWidth / 2 -> BadgePlacement(BadgeSide.LEFT, fraction)
                else -> BadgePlacement(BadgeSide.RIGHT, fraction)
            }
        }
    }
}
