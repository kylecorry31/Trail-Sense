package com.kylecorry.trail_sense.weather.ui

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

class QuickActionThermometer(btn: FloatingActionButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    override fun onCreate() {
        button.setImageResource(R.drawable.thermometer)
        UiUtils.setButtonState(
            button,
            false,
            Resources.color(context, R.color.colorPrimary),
            Resources.color(context, R.color.colorSecondary)
        )
        button.setOnClickListener {
            fragment.findNavController()
                .navigate(R.id.action_action_weather_to_thermometerFragment)
        }
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onDestroy() {
    }

}