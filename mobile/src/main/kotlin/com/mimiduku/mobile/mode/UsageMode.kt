package com.mimiduku.mobile.mode

import com.mimiduku.mobile.detect.ExternalTransition
import com.mimiduku.mobile.feature.FeatureSet
import com.mimiduku.mobile.log.InterventionAction

/**
 * 利用モード。広告対策は大人も使うため、利用者に合わせて介入の強さと文言を切り替える（MVP_DESIGN.md 4章）。
 */
enum class UsageMode {
    /** 外部遷移を自動で戻し、ひらがな中心の文言で警告する。 */
    CHILD,

    /** 自動では戻さず、「戻る」「このまま進む」を出して利用者に選ばせる。通常の文言で警告する。 */
    ADULT,
    ;

    companion object {
        /** 切替を忘れても保護が弱くならない側に倒すため、既定はこどもモード。 */
        val DEFAULT = CHILD

        fun fromStored(value: String?): UsageMode = entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

/** 介入の内容。[mode] は警告文言の種類の選択にも使う。 */
data class Intervention(
    val action: InterventionAction,
    val mode: UsageMode,
)

/** 利用モードと検知内容から、行う介入を決める。 */
object InterventionPolicy {
    /** 同じ遷移元からの繰り返しで、自動で戻す回数の上限。超えたら警告だけにする（MVP_DESIGN.md 5章）。 */
    const val MAX_AUTO_BACK_IN_WINDOW = 3

    /**
     * 外部遷移への介入。[features] で自動で戻すことが無効（Play 版）なら、こどもモードでも利用者に選ばせる。
     * 介入するかどうか（タップのあった遷移を対象にするか）は [shouldIntervene] で先に判定する。
     */
    fun forExternalTransition(
        mode: UsageMode,
        transition: ExternalTransition,
        features: FeatureSet = FeatureSet.GITHUB,
    ): Intervention {
        val action =
            when {
                mode == UsageMode.ADULT || !features.autoBackOnTransition -> InterventionAction.ASK_USER
                else ->
                    if (transition.repeatCount <= MAX_AUTO_BACK_IN_WINDOW) {
                        InterventionAction.BACK_AND_WARN
                    } else {
                        InterventionAction.WARN_ONLY
                    }
            }
        return Intervention(action, mode)
    }

    /**
     * 外部遷移に介入するか。タップのあった遷移は通常の広告クリックを含むため、[FeatureSet.interveneOnTappedTransition]
     * が無効（Play 版）なら介入しない。タップなしの遷移は悪質さのシグナルとして常に介入する。
     */
    fun shouldIntervene(
        transition: ExternalTransition,
        features: FeatureSet,
    ): Boolean = features.interveneOnTappedTransition || !transition.clickedRecently

    /** スケアウェアはどちらのモードでも警告だけを出す（画面を閉じる判断は利用者に任せる）。 */
    fun forScareware(mode: UsageMode): Intervention = Intervention(InterventionAction.WARN_ONLY, mode)
}
