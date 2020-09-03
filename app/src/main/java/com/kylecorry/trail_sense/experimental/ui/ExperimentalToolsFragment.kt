package com.kylecorry.trail_sense.experimental.ui

import android.app.AlertDialog
import android.hardware.SensorManager
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.calibration.infrastructure.AltimeterCalibrator
import com.kylecorry.trail_sense.experimental.inclinometer.ui.InclinometerFragment
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.shared.system.UiUtils
import kotlin.math.roundToInt


class ExperimentalToolsFragment : PreferenceFragmentCompat() {

    private lateinit var inclinometerPref: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.experimental_tools, rootKey)

        bindPreferences()
    }

    private fun bindPreferences(){
        inclinometerPref = findPreference(getString(R.string.experimental_tool_inclinometer))!!


        inclinometerPref.setOnPreferenceClickListener {
            switchToFragment(InclinometerFragment(), addToBackStack = true)
            false
        }

    }


}