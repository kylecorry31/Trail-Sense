package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

abstract class BaseMultipartUnitInputView<T, Units : Enum<*>>(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    protected abstract fun createDisplayUnit(units: Units): MultipartUnitInputView.DisplayUnit<Units>
    protected abstract fun map(amount: Number, secondaryAmount: Number?, unit: Units): T
    protected abstract fun getAmount(value: T): Number
    protected abstract fun getSecondaryAmount(value: T): Number?
    protected abstract fun getUnit(value: T): Units

    var units: List<Units> = listOf()
        set(value) {
            field = value
            unitInput.units = value.map {
                createDisplayUnit(it)
            }
            if (unitInput.unit == null) {
                unitInput.unit = value.firstOrNull()
            }
        }

    override fun isEnabled(): Boolean {
        return unitInput.isEnabled
    }

    override fun setEnabled(enabled: Boolean) {
        unitInput.isEnabled = enabled
    }

    var hint: CharSequence?
        get() = unitInput.hint
        set(value) {
            unitInput.hint = value
        }

    var secondaryHint: CharSequence?
        get() = unitInput.secondaryHint
        set(value) {
            unitInput.secondaryHint = value
        }

    var unit: Units?
        get() = unitInput.unit
        set(value) {
            unitInput.unit = value
        }

    var showSecondaryAmount: Boolean
        get() = unitInput.showSecondaryAmount
        set(value) {
            val changed = value != unitInput.showSecondaryAmount
            unitInput.showSecondaryAmount = value
            if (changed) {
                refreshTextFields()
            }
        }

    var value: T?
        get() {
            val amount = unitInput.amount
            val secondaryAmount = unitInput.secondaryAmount
            val unit = unitInput.unit
            if (amount == null || unit == null) {
                return null
            }
            return map(amount, secondaryAmount, unit)
        }
        set(value) {
            unitInput.amount = if (value == null) null else getAmount(value)
            unitInput.secondaryAmount = if (value == null) null else getSecondaryAmount(value)
            unitInput.unit = if (value == null) null else getUnit(value)
        }

    private var changeListener: ((value: T?) -> Unit)? = null

    private var unitInput: MultipartUnitInputView<Units> = MultipartUnitInputView(context)

    init {
        unitInput.onChange = { _, _, _ ->
            changeListener?.invoke(value)
        }

        @Suppress("LeakingThis")
        addView(unitInput)
    }

    private fun refreshTextFields(){
        val value = value
        unitInput.setAmountEditText(if (value == null) null else getAmount(value))
        unitInput.setSecondaryAmountEditText(if (value == null) null else getSecondaryAmount(value))
    }

    open fun setOnValueChangeListener(listener: ((value: T?) -> Unit)?) {
        changeListener = listener
    }
}