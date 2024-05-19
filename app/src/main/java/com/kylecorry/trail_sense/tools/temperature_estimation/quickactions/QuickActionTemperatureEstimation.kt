package com.kylecorry.trail_sense.tools.temperature_estimation.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class QuickActionTemperatureEstimation(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.thermometer)
    }

    override fun onClick() {
        super.onClick()
        fragment.findNavController().navigateWithAnimation(R.id.temperatureEstimationFragment)
    }
}