package com.kylecorry.trail_sense.tools.astronomy

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.test_utils.TestUtils
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

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setupApplication()
        val scenario = TestUtils.startWithTool(Tools.ASTRONOMY)
        scenario.onActivity {
            navController = it.findNavController()
        }
    }

    @Test
    fun basicFunctionality() {
        // Verify the title
        TestUtils.hasText(R.id.astronomy_title) {
            val valid = listOf(
                TestUtils.getString(R.string.until_sunset),
                TestUtils.getString(R.string.until_sunrise)
            )
            valid.contains(it)
        }

        TestUtils.hasText(R.id.astronomy_title) {
            val regex = Regex("([0-9]+h)? ?([0-9]+m)?")
            regex.matches(it)
        }

        // Verify that today is selected
        TestUtils.hasText(R.id.display_date, R.string.today)

        // Verify the list of astronomy events is displayed
        TestUtils.hasText(R.id.astronomy_detail_list) {
            it.startsWith(TestUtils.getString(R.string.sun))
        }

        TestUtils.hasText(R.id.astronomy_detail_list) {
            it.startsWith(TestUtils.getString(R.string.moon))
        }

        // Verify the View in 3D button is visible and works
        TestUtils.click(R.id.button_3d)
        TestUtils.waitFor {
            assertTrue(
                Tools.getTool(TestUtils.context, Tools.AUGMENTED_REALITY)
                    ?.isOpen(navController.currentDestination?.id ?: 0) == true
            )
        }
    }
}