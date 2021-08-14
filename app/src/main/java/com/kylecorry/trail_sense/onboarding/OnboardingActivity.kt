package com.kylecorry.trail_sense.onboarding

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ActivityOnboardingBinding
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils


class OnboardingActivity : AppCompatActivity() {

    private val cache by lazy { Cache(this) }
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
        val pageToLoad = if (page == OnboardingPages.WEATHER && !SensorChecker(this).hasBarometer()) {
            page + 1
        } else {
            page
        }

        binding.pageSettings.removeAllViews()

        if (page == OnboardingPages.NAVIGATION){
            val backtrackSwitch = SwitchCompat(this)
            backtrackSwitch.isChecked = prefs.backtrackEnabled
            backtrackSwitch.text = getString(R.string.tool_backtrack_title)
            backtrackSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                prefs.backtrackEnabled = isChecked
            }
            binding.pageSettings.addView(backtrackSwitch)
        }

        if (page == OnboardingPages.ASTRONOMY){
            val sunsetAlertSwitch = SwitchCompat(this)
            sunsetAlertSwitch.isChecked = prefs.astronomy.sendSunsetAlerts
            sunsetAlertSwitch.text = getString(R.string.pref_sunset_alerts_title)
            sunsetAlertSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                prefs.astronomy.sendSunsetAlerts = isChecked
            }
            binding.pageSettings.addView(sunsetAlertSwitch)
        }

        if (page == OnboardingPages.WEATHER){
            val weatherSwitch = SwitchCompat(this)
            weatherSwitch.isChecked = prefs.weather.shouldMonitorWeather
            weatherSwitch.text = getString(R.string.pref_monitor_weather_title)
            weatherSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                prefs.weather.shouldMonitorWeather = isChecked
            }
            binding.pageSettings.addView(weatherSwitch)
        }


        pageIdx = pageToLoad

        if (pageToLoad >= OnboardingPages.pages.size) {
            navigateToApp()
        } else {
            val pageContents = OnboardingPages.pages[pageToLoad]
            binding.pageName.text = getString(pageContents.title)
            binding.pageImage.setImageResource(pageContents.image)
            binding.pageImage.imageTintList =
                ColorStateList.valueOf(UiUtils.androidTextColorPrimary(this))
            markdown.setMarkdown(binding.pageContents, getString(pageContents.contents))
        }
    }

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
