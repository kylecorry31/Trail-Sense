package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.toDoubleCompat
import com.kylecorry.andromeda.pickers.Pickers

open class MultipartUnitInputView<Units : Enum<*>>(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var _unit: Units? = null
    private var _amount: Number? = null
    private var _secondaryAmount: Number? = null
    private var _showSecondary: Boolean = false

    override fun isEnabled(): Boolean {
        return amountEdit.isEnabled
    }

    override fun setEnabled(enabled: Boolean) {
        amountEdit.isEnabled = enabled
        secondaryAmountEdit.isEnabled = enabled
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
        get() = amountEdit.hint
        set(value) {
            amountEdit.hint = value
        }

    var secondaryHint: CharSequence?
        get() = secondaryAmountEdit.hint
        set(value) {
            secondaryAmountEdit.hint = value
        }

    var showSecondaryAmount: Boolean
        get() = _showSecondary
        set(value) {
            _showSecondary = value
            secondaryAmountEdit.isVisible = value
        }

    var unitPickerTitle: CharSequence = ""


    var onChange: ((amount: Number?, secondaryAmount: Number?, unit: Units?) -> Unit)? = null

    private var amountEdit = EditText(context)
    private var secondaryAmountEdit = EditText(context)
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
        amountEdit.inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED

        secondaryAmountEdit.inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL

        val editParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        amountEdit.layoutParams = editParams

        val secondaryEditParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        secondaryEditParams.marginStart = Resources.dp(context, 8f).toInt()
        secondaryAmountEdit.layoutParams = secondaryEditParams

        secondaryAmountEdit.isVisible = _showSecondary

        unitBtn = Button(context)
        val btnParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        btnParams.marginStart = Resources.dp(context, 8f).toInt()
        unitBtn.layoutParams = btnParams
        unitBtn.isAllCaps = false

        addView(amountEdit)
        addView(secondaryAmountEdit)
        addView(unitBtn)

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