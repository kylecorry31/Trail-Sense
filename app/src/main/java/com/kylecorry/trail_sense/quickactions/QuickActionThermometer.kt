package com.kylecorry.trail_sense.quickactions

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionThermometer(btn: FloatingActionButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    override fun onCreate() {
        button.setImageResource(R.drawable.thermometer)
        CustomUiUtils.setButtonState(button, false)
        button.setOnClickListener {
            fragment.findNavController()
                .navigate(if (Sensors.hasHygrometer(context)) R.id.action_weather_to_temperature_humidity else R.id.action_action_weather_to_thermometerFragment)
        }
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onDestroy() {
    }

}