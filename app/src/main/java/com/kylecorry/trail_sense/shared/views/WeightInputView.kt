package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.floor

class WeightInputView(context: Context, attributeSet: AttributeSet? = null) :
    BaseMultipartUnitInputView<Weight, WeightUnits>(context, attributeSet) {

    private val formatService by lazy { FormatService.getInstance(context) }

    var defaultHint: String = context.getString(R.string.weight)

    var showPoundsAndOunces: Boolean = false
        set(value) {
            field = value
            showSecondaryAmount = value && unit == WeightUnits.Pounds
            if (showSecondaryAmount) {
                hint = context.getString(R.string.pounds)
                secondaryHint = context.getString(R.string.ounces_weight)
            } else {
                hint = defaultHint
            }
        }

    init {
        hint = defaultHint
        allowNegative = false
        setOnValueChangeListener(null)
    }

    override fun setOnValueChangeListener(listener: ((value: Weight?) -> Unit)?) {
        super.setOnValueChangeListener {
            val wasShowing = showSecondaryAmount
            showSecondaryAmount = showPoundsAndOunces && unit == WeightUnits.Pounds
            if (!wasShowing && showSecondaryAmount) {
                hint = context.getString(R.string.pounds)
                secondaryHint = context.getString(R.string.ounces_weight)
            } else if (wasShowing && !showSecondaryAmount) {
                hint = defaultHint
            } else {
                listener?.invoke(it)
            }
        }
    }

    override fun createDisplayUnit(units: WeightUnits): MultipartUnitInputView.DisplayUnit<WeightUnits> {
        return MultipartUnitInputView.DisplayUnit(
            units,
            formatService.getWeightUnitName(units, true),
            formatService.getWeightUnitName(units)
        )
    }

    override fun getAmount(value: Weight): Number {
        return if (showSecondaryAmount) {
            floor(value.value)
        } else {
            value.value
        }
    }

    override fun getUnit(value: Weight): WeightUnits {
        return value.units
    }

    override fun map(amount: Number, secondaryAmount: Number?, unit: WeightUnits): Weight {
        return if (showSecondaryAmount) {
            Weight.from(amount.toFloat() + (secondaryAmount?.toFloat() ?: 0f) / 16f, unit)
        } else {
            Weight.from(amount.toFloat(), unit)
        }
    }

    override fun getSecondaryAmount(value: Weight): Number? {
        return if (showSecondaryAmount) {
            val ounces = (value.value % 1f) * 16
            if (ounces == 0f) {
                null
            } else {
                ounces
            }
        } else {
            null
        }
    }

}
