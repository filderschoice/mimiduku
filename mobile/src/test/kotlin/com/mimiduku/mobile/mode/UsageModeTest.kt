package com.mimiduku.mobile.mode

import com.mimiduku.mobile.detect.ExternalTransition
import com.mimiduku.mobile.detect.TransitionTarget
import com.mimiduku.mobile.log.InterventionAction
import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageModeTest {
    @Test
    fun `the default mode is child`() {
        assertEquals(UsageMode.CHILD, UsageModeStore(MemoryStringStore()).load())
    }

    @Test
    fun `a saved mode is loaded back`() {
        val store = MemoryStringStore()

        UsageModeStore(store).save(UsageMode.ADULT)

        assertEquals(UsageMode.ADULT, UsageModeStore(store).load())
    }

    @Test
    fun `an unknown stored value falls back to the default`() {
        val store = MemoryStringStore().apply { values["usage_mode"] = "TEEN" }

        assertEquals(UsageMode.CHILD, UsageModeStore(store).load())
    }

    @Test
    fun `child mode goes back automatically up to the limit`() {
        val actions =
            (1..InterventionPolicy.MAX_AUTO_BACK_IN_WINDOW + 1).map {
                InterventionPolicy.forExternalTransition(UsageMode.CHILD, transition(repeatCount = it)).action
            }

        assertEquals(
            List(InterventionPolicy.MAX_AUTO_BACK_IN_WINDOW) { InterventionAction.BACK_AND_WARN } +
                InterventionAction.WARN_ONLY,
            actions,
        )
    }

    @Test
    fun `adult mode asks the user instead of going back`() {
        val intervention = InterventionPolicy.forExternalTransition(UsageMode.ADULT, transition(repeatCount = 1))

        assertEquals(Intervention(InterventionAction.ASK_USER, UsageMode.ADULT), intervention)
    }

    @Test
    fun `scareware only warns in both modes`() {
        UsageMode.entries.forEach { mode ->
            assertEquals(Intervention(InterventionAction.WARN_ONLY, mode), InterventionPolicy.forScareware(mode))
        }
    }

    private fun transition(repeatCount: Int) =
        ExternalTransition(
            sourcePackage = "com.example.game",
            targetPackage = "com.android.chrome",
            target = TransitionTarget.BROWSER,
            clickedRecently = false,
            repeatCount = repeatCount,
        )
}
