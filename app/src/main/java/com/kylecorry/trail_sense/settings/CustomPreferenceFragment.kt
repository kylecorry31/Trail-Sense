package com.kylecorry.trail_sense.settings

import androidx.annotation.StringRes
import androidx.preference.*

abstract class CustomPreferenceFragment: PreferenceFragmentCompat() {

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

}