package com.kylecorry.trail_sense.onboarding

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ActivityOnboardingBinding
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService


class OnboardingActivity : AppCompatActivity() {

    private val cache by lazy { PreferencesSubsystem.getInstance(this).preferences }
    private val markdown by lazy { MarkdownService(this) }
    private val sensors by lazy { SensorService(this) }

    private lateinit var binding: ActivityOnboardingBinding

    private var pageIdx = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        load(pageIdx)

        binding.nextButton.setOnClickListener {
            var nextPage = pageIdx + 1

            // Skip the missing compass page if the compass is available
            if (nextPage == OnboardingPages.MISSING_COMPASS && sensors.hasCompass()){
                nextPage++
            }
            
            load(nextPage)
        }

    }

    private fun navigateToApp() {
        // Consider the disclaimer as shown
        cache.putBoolean(getString(R.string.pref_main_disclaimer_shown_key), true)
        cache.putBoolean(getString(R.string.pref_onboarding_completed), true)
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        pageIdx = savedInstanceState.getInt("page", 0)
        if (pageIdx >= OnboardingPages.pages.size || pageIdx < 0) {
            pageIdx = 0
        }
        load(pageIdx)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", pageIdx)
    }

    private fun load(page: Int) {
        binding.pageSettings.removeAllViews()

        pageIdx = page

        if (page >= OnboardingPages.pages.size) {
            navigateToApp()
        } else {
            val pageContents = OnboardingPages.pages[page]
            binding.pageName.title.text = getString(pageContents.title)
            binding.pageImage.setImageResource(pageContents.image)
            binding.pageImage.imageTintList =
                ColorStateList.valueOf(Resources.androidTextColorPrimary(this))
            binding.nextButton.text = getString(pageContents.nextButtonText)
            markdown.setMarkdown(binding.pageContents, getString(pageContents.contents))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount

        if (count == 0) {
            super.onBackPressed()
            //additional code
        } else {
            supportFragmentManager.popBackStackImmediate()
        }
    }

}
