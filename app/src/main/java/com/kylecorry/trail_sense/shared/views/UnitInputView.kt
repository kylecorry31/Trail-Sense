package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.core.toDoubleCompat
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R

open class UnitInputView<Units : Enum<*>>(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var _unit: Units? = null
    private var _amount: Number? = null

    override fun isEnabled(): Boolean {
        return amountEdit.isEnabled
    }

    override fun setEnabled(enabled: Boolean) {
        amountEdit.isEnabled = enabled
        unitBtn.isEnabled = enabled
    }

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
        get() = amountEditHolder.hint
        set(value) {
            amountEditHolder.hint = value
        }

    var unitPickerTitle: CharSequence = ""


    var onChange: ((amount: Number?, unit: Units?) -> Unit)? = null

    private var amountEdit: TextInputEditText
    private var amountEditHolder: TextInputLayout
    private var unitBtn: Button

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
        inflate(context, R.layout.view_unit_input, this)
        amountEditHolder = findViewById(R.id.amount_holder)

        amountEdit = findViewById(R.id.amount)
        amountEdit.inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED

        unitBtn = findViewById(R.id.units)

        unitBtn.isAllCaps = false

        amountEdit.addTextChangedListener {
            _amount = it?.toString()?.toDoubleCompat()
            onChange?.invoke(amount, unit)
        }

        unitBtn.setOnClickListener {
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

    data class DisplayUnit<Units : Enum<*>>(
        val unit: Units,
        val shortName: String,
        val longName: String
    )

}