package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.core.widget.addTextChangedListener
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.units.Weight
import com.kylecorry.trailsensecore.domain.units.WeightUnits


class WeightInputView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var _units = WeightUnits.values().toList()

    var units: List<WeightUnits>
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
        get() = weightEdit.hint
        set(value) {
            weightEdit.hint = value
        }

    var weight: Weight? = null
    private var changeListener: ((weight: Weight?) -> Unit)? = null

    private lateinit var weightEdit: EditText
    private lateinit var unitsSpinner: Spinner

    init {
        context?.let {
            inflate(it, R.layout.view_weight_input, this)
            weightEdit = findViewById(R.id.weight)
            unitsSpinner = findViewById(R.id.units)
            val sets = intArrayOf(R.attr.hint)
            val typedArray = it.obtainStyledAttributes(attrs, sets)
            hint = typedArray.getText(0) ?: it.getString(R.string.weight)
            units = WeightUnits.values().toList()

            weightEdit.addTextChangedListener {
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
        val weightText = weightEdit.text.toString().toFloatOrNull()
        val spinnerIndex = unitsSpinner.selectedItemPosition

        if (weightText == null || spinnerIndex == Spinner.INVALID_POSITION || spinnerIndex >= units.size) {
            weight = null
            changeListener?.invoke(weight)
            return
        }

        weight = Weight(weightText, units[spinnerIndex])
        changeListener?.invoke(weight)
    }

    private fun getUnitName(unit: WeightUnits): String {
        return when (unit) {
            WeightUnits.Pounds -> context.getString(R.string.pounds)
            WeightUnits.Ounces -> context.getString(R.string.ounces_weight)
            WeightUnits.Kilograms -> context.getString(R.string.kilograms)
            WeightUnits.Grams -> context.getString(R.string.grams)
        }
    }

    fun setOnWeightChangeListener(listener: ((weight: Weight?) -> Unit)?) {
        changeListener = listener
    }

    fun updateWeight(weight: Weight?) {
        if (weight != null) {
            weightEdit.setText(DecimalFormatter.format(weight.weight, 4, false))
        } else {
            weightEdit.text = null
        }
        if (weight != null && units.contains(weight.units)) {
            unitsSpinner.setSelection(units.indexOf(weight.units))
        }
        this.weight = weight
    }


}