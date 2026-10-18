package com.mimiduku.mobile.settings

import com.mimiduku.mobile.log.DetectionKind
import com.mimiduku.mobile.log.DetectionLog
import com.mimiduku.mobile.log.DetectionLogEntry
import com.mimiduku.mobile.log.InterventionAction
import com.mimiduku.mobile.mode.BadgeSettings
import com.mimiduku.mobile.mode.ConsentStore
import com.mimiduku.mobile.mode.PauseStore
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.mode.UsageModeStore
import com.mimiduku.mobile.pin.PinCheck
import com.mimiduku.mobile.pin.PinHasher
import com.mimiduku.mobile.pin.PinManager
import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.ZoneOffset

class SettingsControllerTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val store = MemoryStringStore()
    private var now = 1_000_000L
    private val pinManager = PinManager(store, clock = { now }, hasher = PinHasher(iterations = 1_000))
    private val pauseStore = PauseStore(store) { now }

    private val controller by lazy {
        SettingsController(
            SettingsStores(
                modeStore = UsageModeStore(store),
                detectionLog = DetectionLog(logFile),
                pinManager = pinManager,
                pauseStore = pauseStore,
                badgeSettings = BadgeSettings(store),
                consentStore = ConsentStore(store),
            ),
            FakePlatform(),
            zone = { ZoneOffset.UTC },
        )
    }

    private class FakePlatform : SettingsPlatform {
        override fun isServiceEnabled() = true

        override fun showRestrictedGuide() = false

        override fun openAccessibilitySettings() = Unit

        override fun openAppDetails() = Unit

        override fun appLabel(packageName: String) = if (packageName == "com.example.game") "ゲーム" else null
    }

    private val logFile get() = File(folder.root, "log.json")

    @Test
    fun `without a pin the mode switches immediately`() {
        controller.selectMode(UsageMode.ADULT)

        assertEquals(UsageMode.ADULT, controller.state.mode)
        assertNull(controller.dialog)
    }

    @Test
    fun `with a pin switching to adult waits for the pin`() {
        pinManager.set("1234")

        controller.selectMode(UsageMode.ADULT)

        assertEquals(UsageMode.CHILD, controller.state.mode)
        assertTrue(controller.dialog is SettingsDialog.Entry)

        assertEquals(PinCheck.Success, controller.verifyPin("1234"))
        controller.onPinVerified()

        assertEquals(UsageMode.ADULT, controller.state.mode)
        assertNull(controller.dialog)
    }

    @Test
    fun `dismissing the pin dialog cancels the action`() {
        pinManager.set("1234")
        controller.selectMode(UsageMode.ADULT)

        controller.dismissDialog()

        assertEquals(UsageMode.CHILD, controller.state.mode)
    }

    @Test
    fun `switching back to child never asks for the pin`() {
        UsageModeStore(store).save(UsageMode.ADULT)
        pinManager.set("1234")
        controller.refresh()

        controller.selectMode(UsageMode.CHILD)

        assertEquals(UsageMode.CHILD, controller.state.mode)
        assertNull(controller.dialog)
    }

    @Test
    fun `changing the pin asks for the current pin and then opens the setup`() {
        pinManager.set("1234")

        controller.changePin()
        controller.onPinVerified()

        assertEquals(SettingsDialog.Setup, controller.dialog)
        assertTrue(controller.savePin("5678"))
        assertEquals(PinCheck.Success, pinManager.verify("5678"))
    }

    @Test
    fun `an invalid new pin is not saved and keeps the dialog`() {
        controller.setPin()

        assertFalse(controller.savePin("12"))
        assertEquals(SettingsDialog.Setup, controller.dialog)
        assertFalse(controller.state.pinSet)
    }

    @Test
    fun `removing the pin requires the pin`() {
        pinManager.set("1234")

        controller.removePin()
        assertTrue(controller.state.pinSet)

        controller.onPinVerified()
        assertFalse(controller.state.pinSet)
    }

    @Test
    fun `pausing requires the pin and resuming does not`() {
        pinManager.set("1234")

        controller.pause(15)
        assertEquals(0, controller.state.pauseRemainingMinutes)
        controller.onPinVerified()
        assertEquals(15, controller.state.pauseRemainingMinutes)

        controller.resume()
        assertEquals(0, controller.state.pauseRemainingMinutes)
        assertNull(controller.dialog)
    }

    @Test
    fun `the remaining pause time is rounded up to minutes`() {
        assertEquals(0, SettingsController.toMinutesRoundedUp(0))
        assertEquals(1, SettingsController.toMinutesRoundedUp(1))
        assertEquals(1, SettingsController.toMinutesRoundedUp(60_000))
        assertEquals(2, SettingsController.toMinutesRoundedUp(60_001))
    }

    @Test
    fun `the log screen lists entries newest first with app labels`() {
        DetectionLog(logFile).apply {
            append(entry(at = 0, target = "com.android.chrome"))
            append(entry(at = 3_600_000, target = null))
        }

        controller.openLog()

        val rows = controller.logRows!!
        assertEquals(listOf("1970/01/01 01:00:00", "1970/01/01 00:00:00"), rows.map { it.time })
        assertEquals("ゲーム", rows[1].sourceApp)
        assertEquals("com.android.chrome", rows[1].targetApp)
        assertNull(rows[0].targetApp)
    }

    @Test
    fun `clearing the log requires the pin`() {
        DetectionLog(logFile).append(entry(at = 0, target = null))
        pinManager.set("1234")
        controller.openLog()

        controller.clearLog()
        assertEquals(1, controller.logRows!!.size)

        controller.onPinVerified()
        assertTrue(controller.logRows!!.isEmpty())
    }

    @Test
    fun `closing the log returns to the settings`() {
        controller.openLog()

        controller.closeLog()

        assertNull(controller.logRows)
    }

    @Test
    fun `the badge can be hidden without the pin`() {
        pinManager.set("1234")

        controller.setBadgeVisible(false)

        assertFalse(controller.state.badgeVisible)
        assertNull(controller.dialog)
    }

    @Test
    fun `the disclosure is shown until the user accepts it`() {
        assertFalse(controller.state.consented)

        controller.acceptDisclosure()

        assertTrue(controller.state.consented)
        assertTrue(ConsentStore(store).hasConsented())
    }

    @Test
    fun `a consent for an older disclosure version asks again`() {
        store.values["accessibility_consent_version"] = (ConsentStore.CURRENT_VERSION - 1).toString()

        assertFalse(controller.state.consented)
    }

    private fun entry(
        at: Long,
        target: String?,
    ) = DetectionLogEntry(
        atMillis = at,
        kind = if (target == null) DetectionKind.SCAREWARE else DetectionKind.EXTERNAL_TRANSITION,
        sourcePackage = "com.example.game",
        targetPackage = target,
        action = InterventionAction.WARN_ONLY,
    )
}
