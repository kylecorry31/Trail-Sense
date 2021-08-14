package com.kylecorry.trail_sense.settings.ui

import androidx.annotation.IdRes
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment

fun AndromedaPreferenceFragment.navigateOnClick(pref: Preference?, @IdRes action: Int) {
    pref?.setOnPreferenceClickListener {
        findNavController().navigate(action)
        true
    }
}