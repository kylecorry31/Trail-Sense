package com.kylecorry.trail_sense.tools.maps.ui

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionOfflineMaps(
    button: FloatingActionButton,
    fragment: Fragment
) : QuickActionButton(button, fragment) {

    override fun onCreate() {
        button.setImageResource(R.drawable.maps)
        CustomUiUtils.setButtonState(button, false)
        button.setOnClickListener {
            fragment.findNavController().navigate(R.id.mapListFragment)
        }

    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onDestroy() {
    }


}