package com.kylecorry.trail_sense.tools.astronomy

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import androidx.test.core.app.ActivityScenario
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.CustomUiUtils.isDarkThemeOn
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolAstronomyTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.allPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    private lateinit var navController: NavController
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setupApplication()
        scenario = TestUtils.startWithTool(Tools.ASTRONOMY)
        scenario.onActivity {
            navController = it.findNavController()
        }
    }

    @Test
    fun verifyBasicFunctionality() {
        // Verify the title
        waitFor {
            view(R.id.astronomy_title).hasText {
                val valid = listOf(
                    TestUtils.getString(R.string.until_sunset),
                    TestUtils.getString(R.string.until_sunrise)
                )
                valid.contains(it)
            }
        }

        view(R.id.astronomy_title).hasText(Regex("([0-9]+h)? ?([0-9]+m)?"))

        // Verify that today is selected
        view(R.id.display_date).hasText(R.string.today)

        // Verify the list of astronomy events is displayed
        view(R.id.astronomy_detail_list).hasText {
            it.startsWith(TestUtils.getString(R.string.sun))
        }

        view(R.id.astronomy_detail_list).hasText {
            it.startsWith(TestUtils.getString(R.string.moon))
        }

        verifyQuickActions()

        // Verify the View in 3D button is visible and works
        view(R.id.button_3d).click()
        waitFor {
            assertTrue(
                Tools.getTool(TestUtils.context, Tools.AUGMENTED_REALITY)
                    ?.isOpen(navController.currentDestination?.id ?: 0) == true
            )
        }
    }

    @Test
    fun verifyNightMode() {
        TestUtils.openQuickActions()
        quickAction(Tools.QUICK_ACTION_NIGHT_MODE)
            .click()

        waitFor {
            scenario.onActivity {
                assertTrue(it.isDarkThemeOn())
                // TODO: Verify the color filter is applied
            }
        }
    }

    private fun verifyQuickActions() {
        // Verify the sunset alert quick action
        TestUtils.openQuickActions()
        quickAction(Tools.QUICK_ACTION_SUNSET_ALERT)
            .click()
            .click()

        quickAction(Tools.QUICK_ACTION_SUNRISE_ALERT)
            .click()
            .click()

        // TODO: Simulate time passing to verify the alerts are shown

        TestUtils.closeQuickActions()
    }
}