package com.kylecorry.trail_sense.weather.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class QuickActionClouds(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    override fun onCreate() {
        super.onCreate()
        button.setImageResource(R.drawable.cloudy)
        CustomUiUtils.setButtonState(
            button,
            false
        )
        button.setOnClickListener {
            fragment.findNavController().navigateWithAnimation(R.id.cloudFragment)
        }
    }

}