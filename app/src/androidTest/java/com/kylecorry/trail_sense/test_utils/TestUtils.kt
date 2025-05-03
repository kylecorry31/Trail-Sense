package com.kylecorry.trail_sense.test_utils

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.work.testing.WorkManagerTestInitHelper
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.permissions.SpecialPermission
import com.kylecorry.andromeda.torch.TorchStateChangedTopic
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.main.TrailSenseApplicationInitializer
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.views.childWithIndex
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.longClick
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assert.assertTrue
import org.junit.rules.TestRule

object TestUtils {

    var inUseCameraIds: List<String> = emptyList()
        private set

    var isTorchOn: Boolean = false
        private set

    private var cameraAvailabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            inUseCameraIds = inUseCameraIds.filter { it != cameraId }
        }

        override fun onCameraUnavailable(cameraId: String) {
            inUseCameraIds = inUseCameraIds + cameraId
        }
    }

    private var torchTopic: TorchStateChangedTopic? = null

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun mute(): Int {
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, 0, 0)
        return currentVolume
    }

    fun unmute(volume: Int) {
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, volume, 0)
    }

    fun isPlayingMusic(): Boolean {
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        return audioManager.isMusicActive
    }

    fun back(requireSuccess: Boolean = true) {
        if (requireSuccess) {
            assertTrue(device.pressBack())
        } else {
            device.pressBack()
        }
    }

    fun getString(@StringRes id: Int, vararg args: Any): String {
        return context.getString(id, *args)
    }

    fun setWaitForIdleTimeout(timeout: Long = 100) {
        Configurator.getInstance().setWaitForIdleTimeout(timeout)
    }

    fun listenForCameraUsage() {
        val manager = context.getSystemService<CameraManager>()
        manager?.registerAvailabilityCallback(
            cameraAvailabilityCallback,
            Handler(Looper.getMainLooper())
        )
    }

    fun stopListeningForCameraUsage() {
        val manager = context.getSystemService<CameraManager>()
        manager?.unregisterAvailabilityCallback(cameraAvailabilityCallback)
    }

    fun listenForTorchUsage() {
        torchTopic = TorchStateChangedTopic(context)
        torchTopic?.subscribe(this::onTorchStateChanged)
    }

    fun stopListeningForTorchUsage() {
        torchTopic?.unsubscribe(this::onTorchStateChanged)
        torchTopic = null
    }

    fun not(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            return
        }
        throw Exception("Expected exception")
    }

    // STARTUP
    /**
     * Setup the application to match the actual application (Trail Sense application)
     */
    fun setupApplication() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        // Drop the DB
        AppDatabase.getInstance(context).clearAllTables()
        TrailSenseApplicationInitializer.initialize(context)
        setupDefaultPreferences()
    }

    fun setLocationOverride(coordinate: Coordinate) {
        val prefs = UserPreferences(context)
        prefs.useAutoLocation = false
        prefs.locationOverride = coordinate
    }

    fun startWithTool(
        toolId: Long,
        onActivityCallback: (activity: MainActivity) -> Unit = {}
    ): ActivityScenario<MainActivity> {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            onActivityCallback(activity)
            if (toolId == 0L) {
                activity.findNavController().navigate(R.id.action_experimental_tools)
            } else {
                val tool = Tools.getTool(context, toolId)
                tool?.let {
                    activity.findNavController().navigate(tool.navAction)
                }
            }
        }
        return scenario
    }

    fun onMain(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }

    fun openQuickActions() {
        waitFor {
            view(R.id.bottom_navigation).longClick()
        }
        waitFor {
            view(R.id.quick_actions_sheet)
        }
    }

    fun closeQuickActions() {
        click(R.id.close_button)
    }

    // PERMISSIONS
    fun mainPermissionsGranted(): TestRule {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return GrantPermissionRule.grant(*permissions.toTypedArray())
    }

    fun allPermissionsGranted(): TestRule {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return GrantPermissionRule.grant(*permissions.toTypedArray())
    }

    // PREFERENCES
    fun setupDefaultPreferences() {
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        prefs.clear()
        prefs.putString(context.getString(R.string.pref_distance_units), "feet_miles")
        prefs.putString(context.getString(R.string.pref_weight_units), "lbs")
        prefs.putBoolean(context.getString(R.string.pref_use_24_hour), false)
        prefs.putBoolean(context.getString(R.string.pref_onboarding_completed), true)
        prefs.putBoolean(context.getString(R.string.pref_main_disclaimer_shown_key), true)
        prefs.putBoolean(context.getString(R.string.pref_require_satellites), false)
        prefs.putBoolean(context.getString(R.string.pref_cliff_height_enabled), true)
        prefs.putString(context.getString(R.string.pref_altimeter_calibration_mode), "gps")
        prefs.putInt(context.getString(R.string.pref_altimeter_accuracy), 1)

        val userPrefs = UserPreferences(context)
        // The settings tool is the fastest to get to idle, which allows the tests to run faster
        userPrefs.bottomNavigationTools = listOf(Tools.SETTINGS)
        userPrefs.useCompactMode = true
    }

    // WAITING
    fun waitForIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    fun <T> waitFor(durationMillis: Long = 5000, action: () -> T): T {
        var remaining = durationMillis
        val interval = 10L
        var times = 0
        var lastException: Throwable? = null
        while (remaining > 0 || times == 0) {
            try {
                return action()
            } catch (e: Throwable) {
                lastException = e
            }
            Thread.sleep(interval)
            remaining -= interval
            times++
        }
        if (lastException != null) {
            throw lastException
        }
        throw Exception("Timeout")
    }

    // NOTIFICATIONS
    fun openNotificationShade() {
        device.openNotification()
    }

    fun closeNotificationShade() {
        device.swipe(0, device.displayHeight, 0, 0, 10)
    }

    // Helpers
    fun matchesSelfOrChild(
        parent: UiObject2,
        depth: Int = 10,
        predicate: (obj: UiObject2) -> Boolean
    ): Boolean {
        if (depth == 0) {
            return false
        }

        if (predicate(parent)) {
            return true
        }

        for (child in parent.children) {
            if (matchesSelfOrChild(child, depth - 1, predicate)) {
                return true
            }
        }

        return false
    }

    fun getMatchingChild(
        parent: UiObject2,
        depth: Int = 10,
        predicate: (obj: UiObject2) -> Boolean
    ): UiObject2? {
        if (depth == 0) {
            return null
        }

        if (predicate(parent)) {
            return parent
        }

        for (child in parent.children) {
            val matchingChild = getMatchingChild(child, depth - 1, predicate)
            if (matchingChild != null) {
                return matchingChild
            }
        }

        return null
    }

    fun pickDate(year: Int, month: Int, day: Int, waitForClose: Boolean = true) {
        waitFor {
            view(com.google.android.material.R.id.mtrl_picker_header_toggle).click()
        }

        waitFor {
            view(com.google.android.material.R.id.mtrl_picker_text_input_date)
                .childWithIndex(0)
                .childWithIndex(0)
                .input(
                    "${month.toString().padStart(2, '0')}/${
                        day.toString().padStart(2, '0')
                    }/${year}"
                )
        }

        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        if (waitForClose) {
            waitFor {
                not { view(com.google.android.material.R.id.mtrl_calendar_main_pane) }
            }
        }
    }

    fun pickTime(hour: Int, minute: Int, am: Boolean, waitForClose: Boolean = true) {
        waitFor {
            view(com.google.android.material.R.id.material_timepicker_mode_button).click()
        }

        waitFor {
            view(com.google.android.material.R.id.material_hour_text_input).click()
            view(com.google.android.material.R.id.material_hour_text_input)
                .childWithIndex(0)
                .childWithIndex(0)
                .childWithIndex(0)
                .input(
                    hour.toString()
                )
        }

        view(com.google.android.material.R.id.material_minute_text_input).click()

        waitFor {
            view(com.google.android.material.R.id.material_minute_text_input)
                .childWithIndex(0)
                .childWithIndex(0)
                .childWithIndex(0)
                .input(
                    minute.toString()
                )
        }

        if (am) {
            view(com.google.android.material.R.id.material_clock_period_am_button).click()
        } else {
            view(com.google.android.material.R.id.material_clock_period_pm_button).click()
        }

        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        if (waitForClose) {
            waitFor {
                not { viewWithText(android.R.string.ok) }
            }
        }
    }

    fun isCameraInUse(isBackFacing: Boolean? = null): Boolean {
        val manager = context.getSystemService<CameraManager>() ?: return false
        for (cameraId in inUseCameraIds) {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT && (isBackFacing == null || !isBackFacing)) {
                return true
            }
            if (facing == CameraCharacteristics.LENS_FACING_BACK && (isBackFacing == null || isBackFacing)) {
                return true
            }
        }
        return false
    }

    fun clickListItemMenu(label: String, index: Int = 0) {
        click(com.kylecorry.andromeda.views.R.id.menu_btn, index = index)
        click(label)
    }

    fun handleExactAlarmsDialog() {
        // TODO: Add the option to grant the permission
        if (!Permissions.hasPermission(
                context,
                SpecialPermission.SCHEDULE_EXACT_ALARMS
            )
        ) {
            waitFor {
                viewWithText(
                    context.getString(
                        R.string.allow_schedule_exact_alarms,
                        context.getString(R.string.app_name)
                    )
                )
                viewWithText(android.R.string.cancel).click()
            }
            waitFor {
                not {
                    viewWithText(
                        context.getString(
                            R.string.allow_schedule_exact_alarms,
                            R.string.app_name
                        )
                    )
                }
            }
        }
    }

    private fun onTorchStateChanged(isOn: Boolean): Boolean {
        isTorchOn = isOn
        return true
    }
}