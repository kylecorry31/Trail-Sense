package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.toDoubleCompat
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R

class MultipartUnitInputView<Units : Enum<*>>(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var _unit: Units? = null
    private var _amount: Number? = null
    private var _secondaryAmount: Number? = null
    private var _showSecondary: Boolean = false

    override fun isEnabled(): Boolean {
        return amountEditHolder.isEnabled
    }

    override fun setEnabled(enabled: Boolean) {
        amountEditHolder.isEnabled = enabled
        secondaryAmountEditHolder.isEnabled = enabled
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
                onChange?.invoke(amount, secondaryAmount, unit)
            }
        }

    var amount: Number?
        get() = _amount
        set(value) {
            val changed = value != _amount
            _amount = value
            if (changed) {
                setAmountEditText(value)
                onChange?.invoke(amount, secondaryAmount, unit)
            }
        }

    var secondaryAmount: Number?
        get() = _secondaryAmount
        set(value) {
            val changed = value != _secondaryAmount
            _secondaryAmount = value
            if (changed) {
                setSecondaryAmountEditText(value)
                onChange?.invoke(amount, secondaryAmount, unit)
            }
        }

    var hint: CharSequence?
        get() = amountEditHolder.hint
        set(value) {
            amountEditHolder.hint = value
        }

    var secondaryHint: CharSequence?
        get() = secondaryAmountEditHolder.hint
        set(value) {
            secondaryAmountEditHolder.hint = value
        }

    var showSecondaryAmount: Boolean
        get() = _showSecondary
        set(value) {
            _showSecondary = value
            secondaryAmountEditHolder.isVisible = value
        }

    private var unitPickerTitle: CharSequence = ""


    var onChange: ((amount: Number?, secondaryAmount: Number?, unit: Units?) -> Unit)? = null

    private var amountEdit: TextInputEditText
    private var amountEditHolder: TextInputLayout
    private var secondaryAmountEdit: TextInputEditText
    private var secondaryAmountEditHolder: TextInputLayout
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

    /**
     * Set the field amount without triggering an on change event
     */
    fun setAmountEditText(amount: Number?) {
        val str = if (amount == null) null else DecimalFormatter.format(amount, 5, false)
        amountEdit.setText(str)
    }

    /**
     * Set the field amount without triggering an on change event
     */
    fun setSecondaryAmountEditText(amount: Number?) {
        val str = if (amount == null) null else DecimalFormatter.format(amount, 5, false)
        secondaryAmountEdit.setText(str)
    }

    init {
        inflate(context, R.layout.view_multi_unit_input, this)
        amountEditHolder = findViewById(R.id.amount_holder)
        amountEdit = findViewById(R.id.amount)
        secondaryAmountEditHolder = findViewById(R.id.secondary_amount_holder)
        secondaryAmountEdit = findViewById(R.id.secondary_amount)
        amountEdit.inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED

        secondaryAmountEdit.inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL

        secondaryAmountEditHolder.isVisible = _showSecondary

        unitBtn = findViewById(R.id.units)
        unitBtn.isAllCaps = false

        amountEdit.addTextChangedListener {
            _amount = it?.toString()?.toDoubleCompat()
            onChange?.invoke(amount, secondaryAmount, unit)
        }

        secondaryAmountEdit.addTextChangedListener {
            _secondaryAmount = it?.toString()?.toDoubleCompat()
            onChange?.invoke(amount, secondaryAmount, unit)
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