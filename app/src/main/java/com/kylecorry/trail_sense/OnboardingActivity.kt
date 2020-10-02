package com.kylecorry.trail_sense

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.system.doTransaction


class OnboardingActivity : AppCompatActivity() {

    private lateinit var nextBtn: Button
    private lateinit var prefs: SharedPreferences

    private val pages = listOf(
        R.layout.fragment_onboarding_navigation,
        R.layout.fragment_onboarding_weather,
        R.layout.fragment_onboarding_astronomy,
        R.layout.fragment_onboarding_background_location
    )

    private var pageIdx = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val hasBarometer = SensorChecker(this).hasBarometer()

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        switchFragment(pages[pageIdx])

        nextBtn = findViewById(R.id.next_button)

        nextBtn.setOnClickListener {
            pageIdx++
            if (!hasBarometer && pageIdx == pages.indexOf(R.layout.fragment_onboarding_weather)){
                pageIdx++
            }
            if (pageIdx >= pages.size) {
                navigateToApp()
            } else {
                switchFragment(pages[pageIdx])
            }
        }

    }

    private fun navigateToApp() {
        prefs.edit {
            putBoolean(getString(R.string.pref_onboarding_completed), true)
        }
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        pageIdx = savedInstanceState.getInt("page", 0)
        if (pageIdx >= pages.size || pageIdx < 0){
            pageIdx = 0
        }
        switchFragment(pages[pageIdx])
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", pageIdx)
    }

    private fun switchFragment(layout: Int) {
        supportFragmentManager.doTransaction {
            this.replace(R.id.fragment_holder, Fragment(layout))
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
