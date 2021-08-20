package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.widget.addTextChangedListener
import com.kylecorry.andromeda.core.math.toDoubleCompat
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R

class UnitInputView<Units : Enum<*>>(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var _unit: Units? = null
    private var _amount: Number? = null

    var units: List<DisplayUnit<Units>> = listOf()
        set(value) {
            field = value
            val unit = this.unit
            if (unit != null && value.none { it.unit == unit }) {
                this.unit = null
            }
        }

    var unit: Units?
        get() = _unit
        set(value) {
            val changed = _unit != value
            _unit = value
            if (changed) {
                setSelectedUnitText(value)
                onChange?.invoke(amount, unit)
            }
        }

    var amount: Number?
        get() = _amount
        set(value) {
            val changed = value != _amount
            _amount = value
            if (changed) {
                setAmountEditText(value)
                onChange?.invoke(amount, unit)
            }
        }

    var hint: CharSequence?
        get() = amountEdit.hint
        set(value) {
            amountEdit.hint = value
        }

    var unitPickerTitle: String = ""


    var onChange: ((amount: Number?, unit: Units?) -> Unit)? = null

    private lateinit var amountEdit: EditText
    private lateinit var unitBtn: Button

    private fun setSelectedUnitText(unit: Units?) {
        if (unit != null) {
            val displayUnit = units.firstOrNull { it.unit == unit }
            if (displayUnit == null) {
                _unit = null
                unitBtn.text = ""
            } else {
                unitBtn.text = displayUnit.shortName
            }
        } else {
            unitBtn.text = ""
        }
    }

    private fun setAmountEditText(amount: Number?) {
        amountEdit.setText(amount.toString())
    }

    init {
        context?.let {
            inflate(it, R.layout.view_unit_input, this)
            amountEdit = findViewById(R.id.amount)
            unitBtn = findViewById(R.id.units)
            amountEdit.addTextChangedListener {
                _amount = it?.toString()?.toDoubleCompat()
                onChange?.invoke(amount, unit)
            }

            unitBtn.setOnClickListener {
                // Show picker
                Pickers.item(
                    getContext(),
                    unitPickerTitle,
                    units.map { it.longName },
                    units.indexOfFirst { it.unit == unit }) { idx ->
                    if (idx != null) {
                        unit = units[idx].unit
                    }
                }
            }
        }
    }

    data class DisplayUnit<Units : Enum<*>>(
        val unit: Units,
        val shortName: String,
        val longName: String
    )

}