package com.kylecorry.trail_sense.weather.ui

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration

class QuickActionClouds(btn: FloatingActionButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    override fun onCreate() {
        button.setImageResource(R.drawable.cloudy)
        UiUtils.setButtonState(
            button,
            false,
            UiUtils.color(context, R.color.colorPrimary),
            UiUtils.color(context, R.color.colorSecondary)
        )
        button.setOnClickListener {
            fragment.findNavController()
                .navigate(R.id.action_action_weather_to_cloudFragment)
        }
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onDestroy() {
    }

}