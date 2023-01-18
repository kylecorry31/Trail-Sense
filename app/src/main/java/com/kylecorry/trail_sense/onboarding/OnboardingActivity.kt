package com.kylecorry.trail_sense.onboarding

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ActivityOnboardingBinding
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.UserPreferences


class OnboardingActivity : AppCompatActivity() {

    private val cache by lazy { Preferences(this) }
    private val markdown by lazy { MarkdownService(this) }
    private val prefs by lazy { UserPreferences(this) }

    private lateinit var binding: ActivityOnboardingBinding

    private var pageIdx = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        load(pageIdx)

        binding.nextButton.setOnClickListener {
            load(++pageIdx)
        }

    }

    private fun navigateToApp() {
        // Disclaimer shown when boolean is false
        cache.putBoolean(getString(R.string.pref_main_disclaimer_shown_key), false)
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

        if (page == OnboardingPages.EXPLORE) {
            addSwitch(getString(R.string.backtrack), prefs.backtrackEnabled) {
                prefs.backtrackEnabled = it
            }
            if (Sensors.hasBarometer(this)) {
                addSwitch(
                    getString(R.string.pref_monitor_weather_title),
                    prefs.weather.shouldMonitorWeather
                ) {
                    prefs.weather.shouldMonitorWeather = it
                }
            }
            addSwitch(getString(R.string.sunset_alerts), prefs.astronomy.sendSunsetAlerts) {
                prefs.astronomy.sendSunsetAlerts = it
            }
        }

        pageIdx = page

        if (page >= OnboardingPages.pages.size) {
            navigateToApp()
        } else {
            val pageContents = OnboardingPages.pages[page]
            binding.pageName.title.text = getString(pageContents.title)
            binding.pageImage.setImageResource(pageContents.image)
            binding.pageImage.imageTintList =
                ColorStateList.valueOf(Resources.androidTextColorPrimary(this))
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

    private fun addSwitch(title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        val switch = SwitchCompat(this)
        switch.isChecked = isChecked
        switch.text = title
        switch.setOnCheckedChangeListener { _, checked ->
            onCheckedChange(checked)
        }
        switch.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = Resources.dp(this@OnboardingActivity, 8f).toInt()
        }
        binding.pageSettings.addView(switch)
    }

}
