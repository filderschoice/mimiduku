package com.mimiduku.mobile.overlay

/** バッジが止まる画面の端。 */
enum class BadgeSide { LEFT, RIGHT }

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
         * ドラッグを離した位置（ウィンドウの左上の座標）から置き場所を決める。バッジの中心が画面の外へ出ていれば
         * その端へ最小化し、そうでなければ近い方の端へ吸着する。最小化したバッジを上下に動かしただけなら最小化のまま。
         */
        fun fromRelease(
            windowX: Int,
            windowY: Int,
            screenWidth: Int,
            screenHeight: Int,
            badgeSize: Int,
        ): BadgePlacement {
            val centerX = windowX + badgeSize / 2
            val range = (screenHeight - badgeSize).coerceAtLeast(1)
            val fraction = (windowY.toFloat() / range).coerceIn(0f, 1f)
            return when {
                centerX < 0 -> BadgePlacement(BadgeSide.LEFT, fraction, minimized = true)
                centerX > screenWidth -> BadgePlacement(BadgeSide.RIGHT, fraction, minimized = true)
                centerX < screenWidth / 2 -> BadgePlacement(BadgeSide.LEFT, fraction)
                else -> BadgePlacement(BadgeSide.RIGHT, fraction)
            }
        }
    }
}
