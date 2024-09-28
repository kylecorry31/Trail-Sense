package com.kylecorry.trail_sense.tools.survival_guide

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.scrollToEnd
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolSurvivalGuideTest {

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
        TestUtils.startWithTool(Tools.SURVIVAL_GUIDE)
    }

    @Test
    fun verifyBasicFunctionality() {
        // Accept the disclaimer
        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        waitFor {
            view(R.id.survival_guide_list_title)
        }

        // Verify it shows each chapter
        val chapters = Chapters.getChapters(context)
        chapters.forEachIndexed { index, chapter ->
            if (index > 4){
                view(R.id.survival_guide_chapters_list).scrollToEnd()
            }

            waitFor {
                viewWithText(chapter.title).click()
            }

            // Wait for the chapter to load
            waitFor {
                view(R.id.guide_name).hasText(chapter.title)
                view(R.id.guide_scroll).hasText { it.isNotEmpty() }
                viewWithText("Be prepared").click()
            }

            back()
        }
    }
}