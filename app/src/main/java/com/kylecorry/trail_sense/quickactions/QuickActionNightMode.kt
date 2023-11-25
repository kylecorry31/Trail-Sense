package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.requireMainActivity

class QuickActionNightMode(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    override fun onCreate() {
        super.onCreate()
        val prefs = UserPreferences(fragment.requireContext())
        button.setImageResource(R.drawable.ic_astronomy)
        CustomUiUtils.setButtonState(button, prefs.theme == UserPreferences.Theme.Night)
        var isSwitching = false
        button.setOnClickListener {
            if (isSwitching){
                return@setOnClickListener
            }
            isSwitching = true

            prefs.theme = if (prefs.theme == UserPreferences.Theme.Night) {
                prefs.lastTheme
            } else {
                prefs.lastTheme = prefs.theme
                UserPreferences.Theme.Night
            }

            fragment.requireMainActivity().reloadTheme()
        }
    }
}