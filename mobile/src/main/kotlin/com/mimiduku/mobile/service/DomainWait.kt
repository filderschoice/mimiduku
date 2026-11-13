package com.mimiduku.mobile.service

/**
 * サイトの除外の判定待ち（[InterventionPlan.AwaitDomain]）を管理する（Android の画面操作は行わない）。
 *
 * 遷移先のブラウザのアドレスバーは、開いた直後は前のページのままのことがある。[SETTLE_MILLIS] 待ってから
 * [readDomain] で読み、[InterventionPlanner.afterDomain] で決めた介入を [carryOut] へ渡す。
 * - 待っている間に次の待ちが来たら、前の待ちはドメインを読まずにその場で確認を出す（確認を省かない側。検知ログを欠かさない。BL-071）。
 * - 待っている間に保護が止まったら（一時停止・同意の取り消し）、待ち終えても確認もログも出さない（FR-12。BL-071）。
 * - 待っている間にサービスが終わったら（[finish]）、ドメインを読まずにログだけ残す（バナーは出さない。BL-081）。
 */
class DomainWait(
    private val planner: InterventionPlanner,
    private val timer: OneShotTimer,
    /** 遷移先のブラウザのパッケージ名から、前面のアドレスバーのドメインを読む。読めなければ null。 */
    private val readDomain: (String) -> String?,
    /** 保護が止まっている（一時停止中または未同意）か。 */
    private val protectionStopped: () -> Boolean,
    private val carryOut: (InterventionPlan) -> Unit,
    /** 検知ログへ1件残す（介入は行わない）。[finish] で使う。 */
    private val record: (Detection) -> Unit,
) {
    private var waiting: InterventionPlan.AwaitDomain? = null

    fun await(plan: InterventionPlan.AwaitDomain) {
        waiting?.let { carryOut(planner.afterDomain(it.detection, domain = null)) }
        waiting = plan
        timer.schedule(SETTLE_MILLIS) {
            waiting = null
            if (!protectionStopped()) {
                carryOut(planner.afterDomain(plan.detection, readDomain(plan.browserPackage)))
            }
        }
    }

    /**
     * サービスの終了時に呼ぶ。待ちの予約を取り消し、待ち中の検知があればドメインを読まずに確認を省かない側の扱い
     * （[InterventionPlanner.afterDomain] にドメイン未取得を渡した結果）でログだけ残す（[record]）。バナーはオーバーレイを
     * 閉じるため出さない。保護が止まっていれば何も残さない（BL-081）。
     */
    fun finish() {
        timer.cancel()
        val plan = waiting ?: return
        waiting = null
        if (!protectionStopped()) {
            record(planner.afterDomain(plan.detection, domain = null).detection)
        }
    }

    companion object {
        /** 外部遷移の直後、アドレスバーのドメインを読むまで待つ時間。 */
        const val SETTLE_MILLIS = 1_000L
    }
}
