package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.core.widget.addTextChangedListener
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits


class DistanceInputView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var _units = DistanceUnits.values().toList()

    var units: List<DistanceUnits>
        get() = _units
        set(value) {
            _units = value
            val adapter = ArrayAdapter(
                context,
                R.layout.spinner_item_plain,
                R.id.item_name,
                value.map { getUnitName(it) })
            unitsSpinner.prompt = hint
            unitsSpinner.adapter = adapter
            unitsSpinner.setSelection(0)
        }

    var hint: CharSequence
        get() = distanceEdit.hint
        set(value) {
            distanceEdit.hint = value
        }

    var distance: Distance? = null
    private var changeListener: ((distance: Distance?) -> Unit)? = null

    private lateinit var distanceEdit: EditText
    private lateinit var unitsSpinner: Spinner

    init {
        context?.let {
            inflate(it, R.layout.view_distance_input, this)
            distanceEdit = findViewById(R.id.distance)
            unitsSpinner = findViewById(R.id.units)
            val sets = intArrayOf(R.attr.hint)
            val typedArray = it.obtainStyledAttributes(attrs, sets)
            hint = typedArray.getText(0) ?: it.getString(R.string.distance_hint)
            units = DistanceUnits.values().toList()

            distanceEdit.addTextChangedListener {
                onChange()
            }

            unitsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    onChange()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    onChange()
                }

            }

            typedArray.recycle()
        }
    }

    private fun onChange() {
        val distanceText = distanceEdit.text.toString().toFloatOrNull()
        val spinnerIndex = unitsSpinner.selectedItemPosition

        if (distanceText == null || spinnerIndex == Spinner.INVALID_POSITION || spinnerIndex >= units.size) {
            distance = null
            changeListener?.invoke(distance)
            return
        }

        distance = Distance(distanceText, units[spinnerIndex])
        changeListener?.invoke(distance)
    }

    private fun getUnitName(unit: DistanceUnits): String {
        return when (unit) {
            DistanceUnits.Meters -> context.getString(R.string.unit_meters)
            DistanceUnits.Kilometers -> context.getString(R.string.unit_kilometers)
            DistanceUnits.Feet -> context.getString(R.string.unit_feet)
            DistanceUnits.Miles -> context.getString(R.string.unit_miles)
            DistanceUnits.NauticalMiles -> context.getString(R.string.unit_nautical_miles)
            DistanceUnits.Centimeters -> context.getString(R.string.unit_centimeters)
            DistanceUnits.Inches -> context.getString(R.string.unit_inches)
        }
    }

    fun setOnDistanceChangeListener(listener: ((distance: Distance?) -> Unit)?) {
        changeListener = listener
    }


}