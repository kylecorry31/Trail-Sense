package com.kylecorry.trail_sense.tools.guide

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.not
import com.kylecorry.trail_sense.test_utils.TestUtils.openQuickActions
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.scroll
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import com.kylecorry.trail_sense.tools.guide.infrastructure.Guides
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolUserGuideTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.mainPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        TestUtils.startWithTool(Tools.USER_GUIDE)
    }

    @Test
    fun verifyBasicFunctionality() {
        waitFor {
            view(R.id.searchbox)
        }

        // Verify it shows each guide
        val guides = Guides.guides(context)
            .flatMap { it.guides }
            // TODO: Open each guide, maybe by navigating directly to them
            .take(2)


        guides.forEachIndexed { index, guide ->

            // Skip the weather guide (no good way to differentiate it from the section in this test)
            if (guide.name == "Weather") {
                return@forEachIndexed
            }

            // Wait for the guides to load
            waitFor {
                viewWithText(guides.first().name)
            }

            waitFor {
                viewWithText(guide.name).click()
            }

            // Wait for the guide to load
            waitFor {
                view(R.id.guide_name).hasText(guide.name)
                view(R.id.guide_scroll).hasText { it.isNotEmpty() }
            }

            back()
        }

        // Search
        view(R.id.search_view_edit_text).input("Sett")
        waitFor {
            viewWithText("Settings")
        }

        verifyQuickAction()
    }

    fun verifyQuickAction() {
        openQuickActions()

        quickAction(Tools.QUICK_ACTION_USER_GUIDE)
            .click()

        // Verify it shows the user guide
        waitFor {
            view(R.id.guide_name).hasText("Tools")
            view(R.id.guide_scroll).hasText { it.isNotEmpty() }
        }
    }
}