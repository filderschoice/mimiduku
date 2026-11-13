package com.mimiduku.mobile.service

import com.mimiduku.mobile.detect.ExternalTransition
import com.mimiduku.mobile.exclusion.ExclusionPolicy
import com.mimiduku.mobile.exclusion.ExclusionStore
import com.mimiduku.mobile.log.InterventionAction

/** 検知1件に対してサービスが行うこと。[InterventionPlanner] が決め、サービスはそのとおりに Android を操作する。 */
sealed interface InterventionPlan {
    /** ログに残す検知（確認を省いたときは介入が [InterventionAction.SKIPPED_EXCLUDED] に変わっている）。 */
    val detection: Detection

    /** 確認を出さず、ログだけ残す（除外の登録に当たる）。 */
    data class LogOnly(
        override val detection: Detection,
    ) : InterventionPlan

    /**
     * 遷移先のブラウザ（[browserPackage]）のアドレスバーが落ち着くのを待ってからドメインを読み、
     * [InterventionPlanner.afterDomain] で決め直す（サイトの除外の登録があるとき）。
     */
    data class AwaitDomain(
        override val detection: Detection,
        val browserPackage: String,
    ) : InterventionPlan

    /** 警告を出してログに残す。 */
    data class Present(
        override val detection: Detection,
        /** 警告の前に「戻る」操作を行うか（自動で戻す介入）。 */
        val goBack: Boolean,
        /** 「今後は確認しない」を付けるときの遷移（「戻る／このまま進む」の確認のときだけ）。付けないなら null。 */
        val exclusionTarget: ExternalTransition?,
        /** 待ってから読めたドメイン。読んでいない・読めなかったなら null（ボタンが押された時点で読む）。 */
        val domain: String?,
    ) : InterventionPlan
}

/**
 * 検知1件への介入の内容を決める（Android の画面操作は行わない）。
 *
 * - アプリの除外に当たる検知（[Detection.skipConfirmation]）は、確認を出さずログだけ残す。
 * - 「戻る／このまま進む」の確認で、ブラウザへの遷移かつサイトの除外の登録があれば、ドメインを読むまで待つ。
 * - それ以外は警告を出す。自動で戻す介入なら先に戻り、確認なら「今後は確認しない」を付ける。
 */
class InterventionPlanner(
    private val exclusions: ExclusionStore,
) {
    fun plan(detection: Detection): InterventionPlan {
        val transition = detection.transition
        return when {
            detection.skipConfirmation -> InterventionPlan.LogOnly(detection)
            transition != null && detection.asksUser && ExclusionPolicy.needsDomain(exclusions, transition) ->
                InterventionPlan.AwaitDomain(detection, transition.targetPackage)
            else -> present(detection, domain = null)
        }
    }

    /** ドメインを待った後の決定。[domain] が登録済みなら確認を省いてログだけ、読めない・未登録なら確認を出す。 */
    fun afterDomain(
        detection: Detection,
        domain: String?,
    ): InterventionPlan =
        if (ExclusionPolicy.skipsByDomain(exclusions, domain)) {
            InterventionPlan.LogOnly(detection.asSkipped())
        } else {
            present(detection, domain)
        }

    private fun present(
        detection: Detection,
        domain: String?,
    ) = InterventionPlan.Present(
        detection = detection,
        goBack = detection.intervention.action == InterventionAction.BACK_AND_WARN,
        exclusionTarget = detection.transition?.takeIf { detection.asksUser },
        domain = domain,
    )

    private val Detection.asksUser: Boolean
        get() = intervention.action == InterventionAction.ASK_USER
}
