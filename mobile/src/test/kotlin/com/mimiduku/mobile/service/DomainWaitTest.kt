package com.mimiduku.mobile.service

import com.mimiduku.mobile.detect.ExternalTransition
import com.mimiduku.mobile.detect.TransitionTarget
import com.mimiduku.mobile.exclusion.ExclusionStore
import com.mimiduku.mobile.log.DetectionKind
import com.mimiduku.mobile.log.DetectionLogEntry
import com.mimiduku.mobile.log.InterventionAction
import com.mimiduku.mobile.mode.InterventionPolicy
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainWaitTest {
    private val exclusions =
        ExclusionStore(MemoryStringStore(), saltGenerator = { "salt" }).apply { addDomain(REGISTERED, "1234") }
    private val planner = InterventionPlanner(exclusions)
    private val timer = FakeTimer()
    private var domain: String? = null
    private var stopped = false
    private val carried = mutableListOf<InterventionPlan>()
    private val recorded = mutableListOf<Detection>()
    private var domainReads = 0
    private val wait =
        DomainWait(
            planner = planner,
            timer = timer,
            readDomain = {
                domainReads++
                domain
            },
            protectionStopped = { stopped },
            carryOut = { carried += it },
            record = { recorded += it },
        )

    @Test
    fun `waits before reading the domain`() {
        val plan = awaitPlan(SOURCE_A)
        domain = "other.example"

        wait.await(plan)

        assertEquals(emptyList<InterventionPlan>(), carried)
        assertEquals(DomainWait.SETTLE_MILLIS, timer.delayMillis)
        timer.fire()
        assertEquals(listOf(present(plan, "other.example")), carried)
    }

    @Test
    fun `a registered domain is only logged as excluded`() {
        val plan = awaitPlan(SOURCE_A)
        domain = REGISTERED

        wait.await(plan)
        timer.fire()

        val logged = carried.single()
        assertTrue(logged is InterventionPlan.LogOnly)
        assertEquals(InterventionAction.SKIPPED_EXCLUDED, logged.detection.logEntry.action)
    }

    @Test
    fun `a second transition within the wait keeps the first one in the log`() {
        val first = awaitPlan(SOURCE_A)
        val second = awaitPlan(SOURCE_B)

        wait.await(first)
        wait.await(second)
        // 前の待ちは、ドメインを読まずにその場で確認を出す（確認を省かない側）。
        assertEquals(listOf(present(first, null)), carried)

        domain = "other.example"
        timer.fire()

        assertEquals(listOf(present(first, null), present(second, "other.example")), carried)
        assertEquals(listOf(SOURCE_A, SOURCE_B), carried.map { it.detection.logEntry.sourcePackage })
        // 確認を出したものには、終了時に記録した印を付けない（BL-086）。
        assertTrue(carried.none { it.detection.logEntry.recordedAtServiceEnd })
    }

    @Test
    fun `nothing is shown or logged when protection stops during the wait`() {
        wait.await(awaitPlan(SOURCE_A))
        stopped = true
        domain = "other.example"

        timer.fire()

        assertEquals(emptyList<InterventionPlan>(), carried)
    }

    @Test
    fun `a finished wait is not carried out again by the next transition`() {
        val first = awaitPlan(SOURCE_A)
        val second = awaitPlan(SOURCE_B)
        domain = REGISTERED
        wait.await(first)
        timer.fire()

        wait.await(second)

        assertEquals(1, carried.size)
    }

    @Test
    fun `a wait cut short by the service ending is logged without a banner`() {
        val plan = awaitPlan(SOURCE_A)
        domain = REGISTERED
        wait.await(plan)

        wait.finish()

        // バナーは出さず（サービスの終了でオーバーレイを閉じるため）、ドメインを読まずに確認を省かない側でログだけ残す（BL-081）。
        assertEquals(emptyList<InterventionPlan>(), carried)
        assertEquals(listOf(present(plan, null).detection.atServiceEnd()), recorded)
        // 種別は「利用者に確認」のままで、終了時に記録した印でログの画面の文言を分ける（BL-086）。
        assertEquals(InterventionAction.ASK_USER, recorded.single().logEntry.action)
        assertTrue(recorded.single().logEntry.recordedAtServiceEnd)
        assertEquals(0, domainReads)
        // 予約は取り消されており、後から重ねて記録しない。
        timer.fire()
        assertEquals(1, recorded.size)
        assertEquals(emptyList<InterventionPlan>(), carried)
    }

    @Test
    fun `nothing is logged when protection stopped before the service ends`() {
        wait.await(awaitPlan(SOURCE_A))
        stopped = true

        wait.finish()

        assertEquals(emptyList<Detection>(), recorded)
        assertEquals(emptyList<InterventionPlan>(), carried)
    }

    @Test
    fun `ending the service without a wait logs nothing`() {
        domain = "other.example"
        wait.await(awaitPlan(SOURCE_A))
        timer.fire()

        wait.finish()
        wait.finish()

        assertEquals(1, carried.size)
        assertEquals(emptyList<Detection>(), recorded)
    }

    private fun present(
        plan: InterventionPlan.AwaitDomain,
        domain: String?,
    ) = InterventionPlan.Present(
        plan.detection,
        goBack = false,
        exclusionTarget = plan.detection.transition,
        domain = domain,
    )

    private fun awaitPlan(source: String): InterventionPlan.AwaitDomain {
        val transition =
            ExternalTransition(
                sourcePackage = source,
                targetPackage = CHROME,
                target = TransitionTarget.BROWSER,
                clickedRecently = false,
                repeatCount = 1,
            )
        val intervention = InterventionPolicy.forExternalTransition(UsageMode.ADULT, transition)
        val entry =
            DetectionLogEntry(
                atMillis = 0,
                kind = DetectionKind.EXTERNAL_TRANSITION,
                sourcePackage = source,
                targetPackage = CHROME,
                action = intervention.action,
                clickedRecently = false,
                usageMode = UsageMode.ADULT,
            )
        val plan = planner.plan(Detection(intervention, entry, transition))
        return plan as InterventionPlan.AwaitDomain
    }

    /** [DelayedAction] と同じく、新しく予約すると前の予約を取り消す。[fire] で待ち時間が過ぎたことにする。 */
    private class FakeTimer : OneShotTimer {
        private var action: (() -> Unit)? = null
        var delayMillis: Long? = null

        override fun schedule(
            delayMillis: Long,
            action: () -> Unit,
        ) {
            cancel()
            this.delayMillis = delayMillis
            this.action = action
        }

        override fun cancel() {
            action = null
        }

        fun fire() {
            val pending = action ?: return
            action = null
            pending()
        }
    }

    private companion object {
        const val SOURCE_A = "com.example.game"
        const val SOURCE_B = "com.example.puzzle"
        const val CHROME = "com.android.chrome"
        const val REGISTERED = "example.com"
    }
}
