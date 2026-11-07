package com.mimiduku.mobile.exclusion

import com.mimiduku.mobile.detect.ExternalTransition
import com.mimiduku.mobile.detect.TransitionTarget

/** 確認ダイアログを省く登録の候補。ダイアログの「今後は確認しない」から設定画面へ渡す。 */
data class ExclusionCandidate(
    val sourcePackage: String,
    /** 遷移先のブラウザで読めたドメイン。読めなければ null。 */
    val domain: String?,
)

/**
 * 「戻る／このまま進む」の確認（[com.mimiduku.mobile.log.InterventionAction.ASK_USER]）を省くかの判定。
 *
 * 省くのは次の場合だけ。自動で戻す介入や警告のみの介入、スケアウェアの警告は対象外。
 * - アプリ: 遷移元が登録済み。インストーラーへの遷移は、導入される内容が危険なため対象外にする。
 * - サイト: ブラウザへの遷移で、読めたドメインが登録済み。ストアはドメインを持たないためサイトの対象外。
 */
object ExclusionPolicy {
    fun skipsByPackage(
        store: ExclusionStore,
        transition: ExternalTransition,
    ): Boolean = transition.target != TransitionTarget.INSTALLER && store.hasPackage(transition.sourcePackage)

    /** ドメインの確認が要るか（ブラウザへの遷移で、サイトの登録があるとき）。 */
    fun needsDomain(
        store: ExclusionStore,
        transition: ExternalTransition,
    ): Boolean = transition.target == TransitionTarget.BROWSER && store.domainCount() > 0

    /** [domain] は [DomainNormalizer.fromUrlText] で整えた値。読めなかった（null）ときは省かない。 */
    fun skipsByDomain(
        store: ExclusionStore,
        domain: String?,
    ): Boolean = domain != null && store.hasDomain(domain)
}
