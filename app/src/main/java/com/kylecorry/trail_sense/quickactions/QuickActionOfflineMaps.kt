package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionOfflineMaps(
    button: ImageButton,
    fragment: Fragment
) : QuickActionButton(button, fragment) {

    override fun onCreate() {
        super.onCreate()
        button.setImageResource(R.drawable.maps)
        CustomUiUtils.setButtonState(button, false)
        button.setOnClickListener {
            fragment.findNavController().navigate(R.id.mapListFragment)
        }

    }
}