package com.kylecorry.trail_sense

import android.Manifest
import android.os.Build
import android.util.Log
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.delay
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ScreenshotFailureRule
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clearAppData
import com.kylecorry.trail_sense.test_utils.TestUtils.closeApp
import com.kylecorry.trail_sense.test_utils.TestUtils.grantPermission
import com.kylecorry.trail_sense.test_utils.views.viewWithResourceId
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Rule
import org.junit.Test

class SmokeTest {

    @get:Rule
    val screenshotRule: ScreenshotFailureRule = ScreenshotFailureRule()

    private val packageName = "com.kylecorry.trail_sense.staging"

    private fun grantBasicPermissions(packageName: String) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.ACTIVITY_RECOGNITION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        for (permission in permissions) {
            grantPermission(packageName, permission)
        }
    }

    // Tools that are the most important w/ actions or most likely to break with minification
    private val toolSmokeTests = mapOf<Long, () -> Unit>(
        Tools.PATHS to ::smokeTestPaths,
        Tools.PHOTO_MAPS to ::smokeTestPhotoMaps,
        Tools.CLOUDS to ::smokeTestClouds,
        Tools.SURVIVAL_GUIDE to ::smokeTestSurvivalGuide,
        Tools.FIELD_GUIDE to ::smokeTestFieldGuide,
        Tools.USER_GUIDE to ::smokeTestUserGuide,
        Tools.BEACONS to ::smokeTestBeacons
    )

    @Test
    fun smokeTest() {
        if (!Package.isPackageInstalled(TestUtils.context, packageName)) {
            // This is only meant to run on the staging build, use ./gradlew installStaging to run this test
            return
        }

        TestUtils.setWaitForIdleTimeout()
        clearAppData(packageName)
        grantBasicPermissions(packageName)
        TestUtils.launchApp(packageName)
        completeOnboarding()

        // Click on the tools bottom navigation tab
        click({ viewWithResourceId("$packageName:id/action_experimental_tools") })

        // Open each tool
        val tools = Tools.getTools(TestUtils.context).filterNot { it.isExperimental }
        tools.forEach {
            openTool(it.id, it.name)
        }

        // Quick actions / widgets sheet
        longClick({ viewWithResourceId("$packageName:id/action_experimental_tools") })
        hasText("Quick actions")
        click("Widgets")
        delay(250)

        closeApp(packageName)
    }

    private fun completeOnboarding() {
        click("Next")
        click("Next")
        click("Next")
        optional {
            click("Next", waitForTime = 200)
        }
        click("I Agree")
    }

    private fun openTool(id: Long, name: String) {
        Log.i(TAG, "Running smoke test for tool: $name ($id)")
        input("Search", name)
        scrollUntil { click(name, index = 1) }
        delay(200)

        val additionalTests = toolSmokeTests[id]
        additionalTests?.invoke()

        backUntil {
            isVisible({ viewWithResourceId("$packageName:id/tool_searchbox") })
        }
        input(name, "")
    }

    private fun smokeTestPaths() {
        // Backtrack
        click({ viewWithResourceId("$packageName:id/play_btn") })
        // Wait for the battery restriction warning to go away
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy)) }
        }

        // Wait for the path to be created
        hasText("Temporary", waitForTime = GPS_WAIT_FOR_TIMEOUT)

        // Stop backtrack
        click({ viewWithResourceId("$packageName:id/play_btn") })

        // Open the path
        click("Temporary")
        back()
    }

    private fun smokeTestPhotoMaps() {
        clickOk()

        click({ viewWithResourceId("$packageName:id/add_btn") })
        click("Camera")
        click({ viewWithResourceId("$packageName:id/capture_button") })
        input("Name", "Test Map", index = 1)
        clickOk()

        click("Next")
        optional {
            clickOk()
        }

        hasText("Test Map")

        input("Location", "42, -72")
        click("Next")

        click(
            { viewWithResourceId("$packageName:id/calibration_map") },
            xPercent = 0.7f,
            yPercent = 0.3f
        )
        input("Location", "42.1, -72.1")
        click("Done")

        hasText("Test Map")
        back()
    }

    private fun smokeTestClouds() {
        click({ viewWithResourceId("$packageName:id/add_btn") })
        click("Camera")
        click({ viewWithResourceId("$packageName:id/capture_button") })
        // Verify a cloud is selected (the top one)
        isChecked({ viewWithResourceId("$packageName:id/checkbox") })
        back()
    }

    private fun smokeTestSurvivalGuide() {
        clickOk()
        click("Overview")
        scrollUntil { click("Be prepared") }
        back()
    }

    private fun smokeTestFieldGuide() {
        clickOk()
        click("Animal")
        click("Ant")
        hasText("A small insect")
        back()
        back()
    }

    private fun smokeTestUserGuide() {
        click("Flashlight")
        hasText("The Flashlight tool")
        back()
    }

    private fun smokeTestBeacons() {
        click({ viewWithResourceId("$packageName:id/create_btn") })
        click("Beacon", exact = true)
        input("Name", "Test beacon")
        input("Location", "42, -72")
        click({ viewWithResourceId("$packageName:id/andromeda_toolbar_right_button") })
        click("Test beacon")
        click("Navigate")
        clickOk()
        hasText("Test beacon")
        click({ viewWithResourceId("$packageName:id/andromeda_toolbar_right_button", index = 1) })
        click("Yes")
        back()
        back()
        back()
    }

    companion object {
        private const val TAG = "SmokeTest"
    }

}