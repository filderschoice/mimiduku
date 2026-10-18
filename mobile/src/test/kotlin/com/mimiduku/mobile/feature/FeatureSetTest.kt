package com.mimiduku.mobile.feature

import com.mimiduku.mobile.R
import com.mimiduku.mobile.detect.ExternalTransition
import com.mimiduku.mobile.detect.ExternalTransitionDetector
import com.mimiduku.mobile.detect.PackageClassifier
import com.mimiduku.mobile.detect.TransitionTarget
import com.mimiduku.mobile.log.InterventionAction
import com.mimiduku.mobile.mode.InterventionPolicy
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.overlay.BannerAction
import com.mimiduku.mobile.overlay.BannerSpec
import com.mimiduku.mobile.rules.DetectionRules
import com.mimiduku.mobile.rules.ScarewareMatcher
import com.mimiduku.mobile.service.EngineEvent
import com.mimiduku.mobile.service.ProtectionEngine
import com.mimiduku.mobile.settings.childModeDescriptionRes
import com.mimiduku.mobile.settings.distributionLabelRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureSetTest {
    @Test
    fun `the build value selects the feature set`() {
        assertEquals(FeatureSet.GITHUB, FeatureSet.forBuild("github"))
        assertEquals(FeatureSet.PLAY, FeatureSet.forBuild("play"))
    }

    @Test
    fun `an unknown build value falls back to the restricted play set`() {
        assertEquals(FeatureSet.PLAY, FeatureSet.forBuild(""))
        assertEquals(FeatureSet.PLAY, FeatureSet.forBuild("unknown"))
    }

    @Test
    fun `the github set enables everything`() {
        with(FeatureSet.GITHUB) {
            assertTrue(autoBackOnTransition && interveneOnTappedTransition && adScreenPanel)
            assertTrue(tapShield && closeButtonProxyTap && badgeReactsToAds && needsAdScreenDetection)
        }
    }

    @Test
    fun `the play set disables features that may interfere with ads`() {
        with(FeatureSet.PLAY) {
            assertFalse(autoBackOnTransition || interveneOnTappedTransition || adScreenPanel)
            assertFalse(tapShield || closeButtonProxyTap || badgeReactsToAds || needsAdScreenDetection)
        }
    }

    @Test
    fun `the play set asks even in child mode instead of going back`() {
        val intervention =
            InterventionPolicy.forExternalTransition(UsageMode.CHILD, transition(clicked = false), FeatureSet.PLAY)

        assertEquals(InterventionAction.ASK_USER, intervention.action)
    }

    @Test
    fun `the play set ignores tapped transitions and keeps untapped ones`() {
        assertFalse(InterventionPolicy.shouldIntervene(transition(clicked = true), FeatureSet.PLAY))
        assertTrue(InterventionPolicy.shouldIntervene(transition(clicked = false), FeatureSet.PLAY))
        assertTrue(InterventionPolicy.shouldIntervene(transition(clicked = true), FeatureSet.GITHUB))
    }

    @Test
    fun `the play engine does not intervene after a tap in the source app`() {
        val engine = engine(FeatureSet.PLAY)
        engine.handle(EngineEvent.WindowChanged(GAME, 0)) { emptyList() }
        engine.handle(EngineEvent.Clicked(GAME, 500)) { emptyList() }

        assertTrue(engine.handle(EngineEvent.WindowChanged(CHROME, 1_000)) { emptyList() }.isEmpty())
    }

    @Test
    fun `the play engine asks a child with child wording on an untapped transition`() {
        val engine = engine(FeatureSet.PLAY)
        engine.handle(EngineEvent.WindowChanged(GAME, 0)) { emptyList() }

        val detection = engine.handle(EngineEvent.WindowChanged(CHROME, 1_000)) { emptyList() }.single()
        val banner = BannerSpec.from(detection)

        assertEquals(InterventionAction.ASK_USER, detection.intervention.action)
        assertEquals(R.string.child_transition_ask_title, banner.titleRes)
        assertEquals(R.string.child_go_back, banner.primaryLabelRes)
        assertEquals(BannerAction.GO_BACK, banner.primaryAction)
        assertEquals(R.string.child_proceed, banner.secondaryLabelRes)
    }

    @Test
    fun `scareware warnings stay in the play set`() {
        val engine = engine(FeatureSet.PLAY)

        val detections = engine.handle(EngineEvent.WindowChanged(CHROME, 0)) { listOf("ウイルスに感染しています") }

        assertEquals(InterventionAction.WARN_ONLY, detections.single().intervention.action)
    }

    @Test
    fun `the settings wording follows the feature set`() {
        assertEquals(R.string.settings_mode_child_desc, childModeDescriptionRes(FeatureSet.GITHUB))
        assertEquals(R.string.settings_mode_child_desc_ask, childModeDescriptionRes(FeatureSet.PLAY))
        assertEquals(R.string.settings_distribution_github, distributionLabelRes(FeatureSet.GITHUB))
        assertEquals(R.string.settings_distribution_play, distributionLabelRes(FeatureSet.PLAY))
    }

    private fun engine(features: FeatureSet) =
        ProtectionEngine(
            detector = ExternalTransitionDetector(PackageClassifier(DetectionRules.DEFAULT, "com.mimiduku.mobile")),
            matcher = ScarewareMatcher(DetectionRules.DEFAULT.scarewarePhrases),
            modeProvider = { UsageMode.CHILD },
            features = features,
        )

    private fun transition(clicked: Boolean) =
        ExternalTransition(GAME, CHROME, TransitionTarget.BROWSER, clickedRecently = clicked, repeatCount = 1)

    private companion object {
        const val GAME = "com.example.game"
        const val CHROME = "com.android.chrome"
    }
}
