package com.kylecorry.trail_sense.weather.ui

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionClouds(btn: FloatingActionButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    override fun onCreate() {
        button.setImageResource(R.drawable.cloudy)
        CustomUiUtils.setButtonState(
            button,
            false
        )
        button.setOnClickListener {
            fragment.findNavController()
                .navigate(R.id.cloudLogFragment)
        }
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onDestroy() {
    }

}