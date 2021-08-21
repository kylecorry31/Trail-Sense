package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.kylecorry.andromeda.forms.UnitInputView

abstract class BaseUnitInputView<T, Units : Enum<*>>(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    protected abstract fun createDisplayUnit(units: Units): UnitInputView.DisplayUnit<Units>
    protected abstract fun map(amount: Number, unit: Units): T
    protected abstract fun getAmount(value: T): Number
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

    var unit: Units?
        get() = unitInput.unit
        set(value) {
            unitInput.unit = value
        }

    var value: T?
        get() {
            val amount = unitInput.amount
            val unit = unitInput.unit
            if (amount == null || unit == null) {
                return null
            }
            return map(amount, unit)
        }
        set(value) {
            unitInput.amount = if (value == null) null else getAmount(value)
            unitInput.unit = if (value == null) null else getUnit(value)
        }


    private var changeListener: ((value: T?) -> Unit)? = null

    private var unitInput: UnitInputView<Units> = UnitInputView(context)

    init {
        unitInput.onChange = { _, _ ->
            changeListener?.invoke(value)
        }

        @Suppress("LeakingThis")
        addView(unitInput)
    }


    fun setOnValueChangeListener(listener: ((value: T?) -> Unit)?) {
        changeListener = listener
    }
}