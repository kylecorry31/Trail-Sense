package com.kylecorry.trail_sense.settings.ui

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.graphics.drawable.DrawableCompat
import androidx.navigation.fragment.findNavController
import androidx.preference.*

// TODO: Extract this to TS Core and add permission support
abstract class CustomPreferenceFragment : PreferenceFragmentCompat() {

    protected fun switch(@StringRes id: Int): SwitchPreferenceCompat? {
        return preferenceManager.findPreference(getString(id))
    }

    protected fun list(@StringRes id: Int): ListPreference? {
        return preferenceManager.findPreference(getString(id))
    }

    protected fun seekBar(@StringRes id: Int): SeekBarPreference? {
        return preferenceManager.findPreference(getString(id))
    }

    protected fun editText(@StringRes id: Int): EditTextPreference? {
        return preferenceManager.findPreference(getString(id))
    }

    protected fun preference(@StringRes id: Int): Preference? {
        return preferenceManager.findPreference(getString(id))
    }

    protected fun navigateOnClick(pref: Preference?, @IdRes action: Int, bundle: Bundle? = null) {
        pref?.setOnPreferenceClickListener {
            findNavController().navigate(action, bundle)
            false
        }
    }

    protected fun onClick(pref: Preference?, action: (preference: Preference) -> Unit) {
        pref?.setOnPreferenceClickListener {
            action(it)
            true
        }
    }

    protected fun setIconColor(@ColorInt color: Int?){
        setIconColor(preferenceScreen, color)
    }

    protected fun setIconColor(preference: Preference, @ColorInt color: Int?) {
        if (preference is PreferenceGroup) {
            for (i in 0 until preference.preferenceCount) {
                setIconColor(preference.getPreference(i), color)
            }
        } else {
            if (preference.icon != null && color != null) {
                preference.icon.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            } else if (preference.icon != null) {
                DrawableCompat.clearColorFilter(preference.icon)
            }
        }
    }

}