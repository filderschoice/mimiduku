package com.mimiduku.mobile.settings

import com.mimiduku.mobile.exclusion.ExclusionCandidate
import com.mimiduku.mobile.exclusion.ExclusionStore
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

    private val platform = FakePlatform()

    private val controller by lazy {
        SettingsController(
            SettingsStores(
                modeStore = UsageModeStore(store),
                detectionLog = DetectionLog(logFile),
                pinManager = pinManager,
                overrides = ProtectionOverrides(pauseStore, ExclusionStore(store)),
                badgeSettings = BadgeSettings(store),
                consentStore = ConsentStore(store),
            ),
            platform,
            zone = { ZoneOffset.UTC },
        )
    }

    private class FakePlatform : SettingsPlatform {
        var notificationPermissionRequests = 0

        override fun requestNotificationPermission() {
            notificationPermissionRequests++
        }

        override fun isServiceEnabled() = true

        override fun showRestrictedGuide() = false

        override fun openAccessibilitySettings() = Unit

        override fun openAppDetails() = Unit

        override fun openSupportPage() = Unit

        override fun appLabel(packageName: String) = if (packageName == "com.example.game") "ゲーム" else null
    }

    private val logFile get() = File(folder.root, "log.json")

    @Test
    fun `the screen starts from the given initial screen`() {
        val restored =
            SettingsController(
                SettingsStores(
                    modeStore = UsageModeStore(store),
                    detectionLog = DetectionLog(logFile),
                    pinManager = pinManager,
                    overrides = ProtectionOverrides(pauseStore, ExclusionStore(store)),
                    badgeSettings = BadgeSettings(store),
                    consentStore = ConsentStore(store),
                ),
                platform,
                initialScreen = Screen.ABOUT,
            )

        assertEquals(Screen.ABOUT, restored.screen)
        assertEquals(Screen.HOME, controller.screen)
    }

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
    fun `navigation moves between home, settings and about and back`() {
        assertEquals(Screen.HOME, controller.screen)

        controller.openSettings()
        assertEquals(Screen.SETTINGS, controller.screen)
        controller.navigateUp()
        assertEquals(Screen.HOME, controller.screen)

        controller.openAbout()
        assertEquals(Screen.ABOUT, controller.screen)
        controller.navigateUp()
        assertEquals(Screen.HOME, controller.screen)
    }

    @Test
    fun `opening the log from the settings screen keeps pin protection on the guarded actions`() {
        pinManager.set("1234")
        controller.openSettings()

        controller.selectMode(UsageMode.ADULT)

        assertTrue(controller.dialog is SettingsDialog.Entry)
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
    fun `the badge notification is on by default and asks for permission when turned on`() {
        assertTrue(controller.state.badgeNotification)

        controller.setBadgeNotification(false)
        assertFalse(controller.state.badgeNotification)
        assertEquals(0, platform.notificationPermissionRequests)

        controller.setBadgeNotification(true)
        assertTrue(controller.state.badgeNotification)
        assertEquals(1, platform.notificationPermissionRequests)
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

    @Test
    fun `adding an exclusion without a pin asks to set a pin and registers nothing`() {
        controller.proposeExclusion(ExclusionCandidate("com.example.game", "example.com"))
        controller.confirmAddExclusion(addPackage = true, addDomainChecked = true)

        assertTrue(controller.dialog is SettingsDialog.Setup)
        assertTrue(controller.state.excludedPackages.isEmpty())
        assertEquals(0, controller.state.excludedDomainCount)
    }

    @Test
    fun `adding an exclusion requires the pin and registers the chosen items`() {
        pinManager.set("1234")
        controller.proposeExclusion(ExclusionCandidate("com.example.game", "example.com"))
        assertEquals(Screen.SETTINGS, controller.screen)

        controller.confirmAddExclusion(addPackage = true, addDomainChecked = true)
        assertTrue(controller.dialog is SettingsDialog.Entry)
        assertTrue(controller.state.excludedPackages.isEmpty())

        assertEquals(PinCheck.Success, controller.verifyPin("1234"))
        controller.onPinVerified()

        assertEquals(listOf(ExcludedApp("com.example.game", "ゲーム")), controller.state.excludedPackages)
        assertEquals(1, controller.state.excludedDomainCount)
    }

    @Test
    fun `only the chosen item is registered and an unreadable site cannot be registered`() {
        pinManager.set("1234")
        controller.proposeExclusion(ExclusionCandidate("com.example.game", null))
        controller.confirmAddExclusion(addPackage = false, addDomainChecked = true)
        assertNull(controller.dialog)
        assertTrue(controller.state.excludedPackages.isEmpty())
        assertEquals(0, controller.state.excludedDomainCount)

        controller.proposeExclusion(ExclusionCandidate("com.example.game", null))
        controller.confirmAddExclusion(addPackage = true, addDomainChecked = true)
        controller.onPinVerified()

        assertEquals(1, controller.state.excludedPackages.size)
        assertEquals(0, controller.state.excludedDomainCount)
    }

    @Test
    fun `removing exclusions needs no pin`() {
        pinManager.set("1234")
        val exclusions = ExclusionStore(store)
        exclusions.addPackage("com.example.game")
        exclusions.addDomain("example.com", "1234")
        controller.refresh()

        controller.removeExcludedPackage("com.example.game")
        controller.clearExcludedDomains()

        assertNull(controller.dialog)
        assertTrue(controller.state.excludedPackages.isEmpty())
        assertEquals(0, controller.state.excludedDomainCount)
    }

    @Test
    fun `the site list is shown only after the pin and sites can be removed from it`() {
        pinManager.set("1234")
        controller.proposeExclusion(ExclusionCandidate("com.example.game", "example.com"))
        controller.confirmAddExclusion(addPackage = false, addDomainChecked = true)
        controller.verifyPin("1234")
        controller.onPinVerified()
        assertEquals(1, controller.state.excludedDomainListable)

        controller.showExcludedDomains()
        assertTrue(controller.dialog is SettingsDialog.Entry)
        controller.verifyPin("1234")
        controller.onPinVerified()

        val list = controller.dialog as SettingsDialog.DomainList
        assertEquals(listOf("example.com"), list.items.map { it.domain })

        controller.removeExcludedDomain(list.items.single().id)
        assertTrue((controller.dialog as SettingsDialog.DomainList).items.isEmpty())
        assertEquals(0, controller.state.excludedDomainCount)
    }

    @Test
    fun `a wrong pin never lists the sites`() {
        pinManager.set("1234")
        ExclusionStore(store).addDomain("example.com", "1234")
        controller.refresh()

        controller.showExcludedDomains()

        assertTrue(controller.verifyPin("0000") is PinCheck.Wrong)
        assertTrue(controller.dialog is SettingsDialog.Entry)
    }

    @Test
    fun `removing the pin also deletes the registered sites`() {
        pinManager.set("1234")
        ExclusionStore(store).addDomain("example.com", "1234")
        controller.refresh()

        controller.removePin()
        controller.verifyPin("1234")
        controller.onPinVerified()

        assertFalse(controller.state.pinSet)
        assertEquals(0, controller.state.excludedDomainCount)
        assertEquals(0, controller.state.excludedDomainListable)
    }

    @Test
    fun `changing the pin re-encrypts the list so the new pin lists it`() {
        pinManager.set("1234")
        ExclusionStore(store).addDomain("example.com", "1234")
        controller.refresh()

        controller.changePin()
        controller.verifyPin("1234")
        controller.onPinVerified()
        assertTrue(controller.savePin("5678"))

        controller.showExcludedDomains()
        controller.verifyPin("5678")
        controller.onPinVerified()

        assertEquals(listOf("example.com"), (controller.dialog as SettingsDialog.DomainList).items.map { it.domain })
    }
}
