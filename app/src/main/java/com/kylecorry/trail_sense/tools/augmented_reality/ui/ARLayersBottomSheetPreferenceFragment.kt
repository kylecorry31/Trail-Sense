package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.os.Bundle
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import java.time.LocalDate

class ARLayersBottomSheetPreferenceFragment : AndromedaPreferenceFragment() {

    private var onAstronomyDateChange: ((LocalDate?) -> Unit)? = null
    var astronomyOverrideDate: LocalDate? = null
        set(value) {
            field = value
            onAstronomyDateChange?.invoke(value)

            // Update the summary
            tryOrNothing {
                val astronomyDateHolder =
                    preference(R.string.pref_ar_layer_settings_astronomy_date_holder)
                astronomyDateHolder?.summary =
                    formatter.formatRelativeDate(value ?: LocalDate.now())
            }
        }

    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    fun setOnAstronomyDateChangeListener(listener: (LocalDate?) -> Unit) {
        onAstronomyDateChange = listener
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.augmented_reality_layer_sheet_preferences, rootKey)

        val astronomyDateHolder = preference(R.string.pref_ar_layer_settings_astronomy_date_holder)
        astronomyDateHolder?.summary =
            formatter.formatRelativeDate(astronomyOverrideDate ?: LocalDate.now())
        onClick(astronomyDateHolder) {
            CustomUiUtils.pickDate(requireContext(), astronomyOverrideDate ?: LocalDate.now()) {
                if (it != null) {
                    astronomyOverrideDate = it
                }
            }
        }
    }
}