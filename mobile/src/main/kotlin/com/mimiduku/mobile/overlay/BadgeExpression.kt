package com.mimiduku.mobile.overlay

import androidx.annotation.DrawableRes
import com.mimiduku.mobile.R

/** みみづくバッジの表情。画像は res/drawable の同名ファイルを差し替えれば置き換えられる。 */
enum class BadgeExpression(
    @DrawableRes val drawableRes: Int,
) {
    /** 平常時。うとうとしている。 */
    SLEEPY(R.drawable.badge_owl_sleepy),

    /** 広告画面を見張っている。羽角（耳）が立つ。 */
    ALERT(R.drawable.badge_owl_alert),

    /** 危険度の高い検知（外部遷移・偽の警告）の直後。目が大きくなる。 */
    WIDE_EYED(R.drawable.badge_owl_wide),
    ;

    companion object {
        /** 危険度の高い検知の後、目を大きくしておく時間。 */
        const val WIDE_EYED_MILLIS = 10_000L

        /** 表示する表情。バッジを出さない設定か一時停止中なら null。 */
        fun of(
            visible: Boolean,
            paused: Boolean,
            onAdScreen: Boolean,
            lastDangerAtMillis: Long?,
            nowMillis: Long,
        ): BadgeExpression? =
            when {
                !visible || paused -> null
                lastDangerAtMillis != null && nowMillis - lastDangerAtMillis in 0 until WIDE_EYED_MILLIS -> WIDE_EYED
                onAdScreen -> ALERT
                else -> SLEEPY
            }
    }
}
