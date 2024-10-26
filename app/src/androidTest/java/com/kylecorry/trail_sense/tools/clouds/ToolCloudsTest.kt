package com.kylecorry.trail_sense.tools.clouds

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.getString
import com.kylecorry.trail_sense.test_utils.TestUtils.not
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.isChecked
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolCloudsTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.allPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        TestUtils.startWithTool(Tools.CLOUDS)
    }

    @Test
    fun verifyBasicFunctionality() {
        waitFor {
            view(R.id.cloud_list_title).hasText(R.string.clouds)
            view(R.id.cloud_empty_text).hasText(R.string.no_clouds)
        }

        canScanCloudFromCamera()
        canScanCloudFromFile()
        canManuallyEnterCloud()
        verifyQuickAction()
    }

    private fun canScanCloudFromCamera() {
        view(R.id.add_btn).click()

        waitFor {
            viewWithText(R.string.camera).click()
        }

        waitFor {
            view(R.id.capture_button).click()
        }

        waitFor {
            view(R.id.cloud_title).hasText(R.string.clouds)
        }

        // Verify a cloud is selected (the top one)
        waitFor {
            view(com.kylecorry.andromeda.views.R.id.checkbox).isChecked()
        }

        toolbarButton(R.id.cloud_title, Side.Right).click()

        waitFor {
            view(com.kylecorry.andromeda.views.R.id.title).click()
        }

        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        waitFor {
            view(com.kylecorry.andromeda.views.R.id.title)
        }

        // Delete all cloud results
        try {
            while (true) {
                clickListItemMenu(getString(R.string.delete))
                waitFor {
                    viewWithText(android.R.string.ok).click()
                }
            }
        } catch (e: Throwable) {
            // Do nothing
        }

        waitFor {
            view(R.id.cloud_empty_text).hasText(R.string.no_clouds)
        }
    }

    private fun canScanCloudFromFile() {
        view(R.id.add_btn).click()

        waitFor {
            viewWithText(R.string.file).click()
        }

        // The file picker is opened
        waitFor {
            not { view(R.id.add_btn) }
        }

        waitFor {
            waitFor {
                back()
            }
            waitFor {
                view(R.id.cloud_list_title)
            }
        }
    }

    private fun canManuallyEnterCloud() {
        view(R.id.add_btn).click()

        waitFor {
            viewWithText(R.string.manual).click()
        }

        waitFor {
            view(R.id.cloud_title).hasText(R.string.clouds)
        }

        // Select the first cloud type
        waitFor {
            view(com.kylecorry.andromeda.views.R.id.checkbox).click()
        }

        // Verify it is selected
        waitFor {
            view(com.kylecorry.andromeda.views.R.id.checkbox).isChecked()
        }

        toolbarButton(R.id.cloud_title, Side.Right).click()

        // Delete the cloud result
        waitFor {
            clickListItemMenu(getString(R.string.delete))
            waitFor {
                viewWithText(android.R.string.ok).click()
            }
        }

        waitFor {
            view(R.id.cloud_empty_text).hasText(R.string.no_clouds)
        }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        quickAction(Tools.QUICK_ACTION_SCAN_CLOUD).click()

        waitFor {
            view(R.id.capture_button).click()
        }

        waitFor {
            view(R.id.cloud_title).hasText(R.string.clouds)
        }

        // Verify a cloud is selected (the top one)
        waitFor {
            view(com.kylecorry.andromeda.views.R.id.checkbox).isChecked()
        }

        toolbarButton(R.id.cloud_title, Side.Right).click()

        waitFor {
            view(com.kylecorry.andromeda.views.R.id.title)
        }

        // Delete all cloud results
        try {
            while (true) {
                clickListItemMenu(getString(R.string.delete))
                waitFor {
                    viewWithText(android.R.string.ok).click()
                }
            }
        } catch (e: Throwable) {
            // Do nothing
        }

        waitFor {
            view(R.id.cloud_empty_text).hasText(R.string.no_clouds)
        }
    }
}