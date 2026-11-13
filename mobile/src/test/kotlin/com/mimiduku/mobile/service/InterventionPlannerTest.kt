package com.mimiduku.mobile.service

import com.mimiduku.mobile.detect.ExternalTransition
import com.mimiduku.mobile.detect.TransitionTarget
import com.mimiduku.mobile.exclusion.ExclusionStore
import com.mimiduku.mobile.feature.FeatureSet
import com.mimiduku.mobile.log.DetectionKind
import com.mimiduku.mobile.log.DetectionLogEntry
import com.mimiduku.mobile.log.InterventionAction
import com.mimiduku.mobile.mode.InterventionPolicy
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionPlannerTest {
    private val exclusions = ExclusionStore(MemoryStringStore(), saltGenerator = { "salt" })
    private val planner = InterventionPlanner(exclusions)

    @Test
    fun `a detection excluded by the source app is only logged`() {
        val skipped = transitionDetection(UsageMode.ADULT).asSkipped()

        assertEquals(InterventionPlan.LogOnly(skipped), planner.plan(skipped))
    }

    @Test
    fun `a confirmation without registered sites is presented with the exclude button`() {
        val detection = transitionDetection(UsageMode.ADULT)

        assertEquals(
            InterventionPlan.Present(detection, goBack = false, exclusionTarget = detection.transition, domain = null),
            planner.plan(detection),
        )
    }

    @Test
    fun `a confirmation to a browser waits for the domain when sites are registered`() {
        exclusions.addDomain("example.com", PIN)
        val detection = transitionDetection(UsageMode.ADULT)

        assertEquals(InterventionPlan.AwaitDomain(detection, CHROME), planner.plan(detection))
    }

    @Test
    fun `a confirmation to the store does not wait for the domain`() {
        exclusions.addDomain("example.com", PIN)
        val detection = transitionDetection(UsageMode.ADULT, target = TransitionTarget.STORE, targetPackage = STORE)

        assertEquals(
            InterventionPlan.Present(detection, goBack = false, exclusionTarget = detection.transition, domain = null),
            planner.plan(detection),
        )
    }

    @Test
    fun `an automatic back goes back without the exclude button or waiting for the domain`() {
        exclusions.addDomain("example.com", PIN)
        val detection = transitionDetection(UsageMode.CHILD)

        assertEquals(InterventionAction.BACK_AND_WARN, detection.intervention.action)
        assertEquals(
            InterventionPlan.Present(detection, goBack = true, exclusionTarget = null, domain = null),
            planner.plan(detection),
        )
    }

    @Test
    fun `a warning after repeated automatic backs neither goes back nor offers the exclude button`() {
        exclusions.addDomain("example.com", PIN)
        val repeated = InterventionPolicy.MAX_AUTO_BACK_IN_WINDOW + 1
        val detection = transitionDetection(UsageMode.CHILD, repeatCount = repeated)

        assertEquals(InterventionAction.WARN_ONLY, detection.intervention.action)
        assertEquals(
            InterventionPlan.Present(detection, goBack = false, exclusionTarget = null, domain = null),
            planner.plan(detection),
        )
    }

    @Test
    fun `a scareware warning is presented without going back or the exclude button`() {
        exclusions.addDomain("example.com", PIN)
        val detection = scarewareDetection()

        assertEquals(
            InterventionPlan.Present(detection, goBack = false, exclusionTarget = null, domain = null),
            planner.plan(detection),
        )
    }

    @Test
    fun `the child mode of the Play build asks the user and waits for the domain`() {
        exclusions.addDomain("example.com", PIN)
        val play = transitionDetection(UsageMode.CHILD, features = FeatureSet.PLAY)
        val github = transitionDetection(UsageMode.CHILD, features = FeatureSet.GITHUB)

        assertEquals(InterventionPlan.AwaitDomain(play, CHROME), planner.plan(play))
        assertEquals(
            InterventionPlan.Present(github, goBack = true, exclusionTarget = null, domain = null),
            planner.plan(github),
        )
    }

    @Test
    fun `a registered domain skips the confirmation and logs it as excluded`() {
        exclusions.addDomain("example.com", PIN)
        val detection = transitionDetection(UsageMode.ADULT)

        val plan = planner.afterDomain(detection, "example.com")

        assertEquals(InterventionPlan.LogOnly(detection.asSkipped()), plan)
        assertEquals(InterventionAction.SKIPPED_EXCLUDED, plan.detection.logEntry.action)
    }

    @Test
    fun `an unregistered domain is presented with the domain for the exclude button`() {
        exclusions.addDomain("example.com", PIN)
        val detection = transitionDetection(UsageMode.ADULT)

        assertEquals(
            InterventionPlan.Present(
                detection,
                goBack = false,
                exclusionTarget = detection.transition,
                domain = "other.example",
            ),
            planner.afterDomain(detection, "other.example"),
        )
    }

    @Test
    fun `an unreadable domain is presented and the site is read when the button is pressed`() {
        exclusions.addDomain("example.com", PIN)
        val detection = transitionDetection(UsageMode.ADULT)

        assertEquals(
            InterventionPlan.Present(detection, goBack = false, exclusionTarget = detection.transition, domain = null),
            planner.afterDomain(detection, null),
        )
    }

    private fun transitionDetection(
        mode: UsageMode,
        target: TransitionTarget = TransitionTarget.BROWSER,
        targetPackage: String = CHROME,
        repeatCount: Int = 1,
        features: FeatureSet = FeatureSet.GITHUB,
    ): Detection {
        val transition =
            ExternalTransition(
                sourcePackage = GAME,
                targetPackage = targetPackage,
                target = target,
                clickedRecently = false,
                repeatCount = repeatCount,
            )
        val intervention = InterventionPolicy.forExternalTransition(mode, transition, features)
        val entry =
            DetectionLogEntry(
                atMillis = 0,
                kind = DetectionKind.EXTERNAL_TRANSITION,
                sourcePackage = GAME,
                targetPackage = targetPackage,
                action = intervention.action,
                clickedRecently = false,
                usageMode = mode,
            )
        return Detection(intervention, entry, transition)
    }

    private fun scarewareDetection(): Detection {
        val intervention = InterventionPolicy.forScareware(UsageMode.CHILD)
        val entry =
            DetectionLogEntry(
                atMillis = 0,
                kind = DetectionKind.SCAREWARE,
                sourcePackage = CHROME,
                ruleId = "virus",
                action = intervention.action,
                usageMode = UsageMode.CHILD,
            )
        return Detection(intervention, entry)
    }

    private companion object {
        const val GAME = "com.example.game"
        const val CHROME = "com.android.chrome"
        const val STORE = "com.android.vending"
        const val PIN = "1234"
    }
}
