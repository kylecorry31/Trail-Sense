package com.kylecorry.trail_sense.settings.ui

import androidx.annotation.IdRes
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.shared.navigateWithAnimation

fun AndromedaPreferenceFragment.navigateOnClick(pref: Preference?, @IdRes action: Int) {
    pref?.setOnPreferenceClickListener {
        tryOrNothing {
            findNavController().navigateWithAnimation(action)
        }
        true
    }
}