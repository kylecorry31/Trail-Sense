package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.floor

class DistanceInputView(context: Context, attrs: AttributeSet? = null) :
    BaseMultipartUnitInputView<Distance, DistanceUnits>(context, attrs) {

    private val formatService by lazy { FormatService(context) }

    var showFeetAndInches: Boolean = false
        set(value) {
            field = value
            showSecondaryAmount = value && unit == DistanceUnits.Feet
            if (showSecondaryAmount) {
                hint = context.getString(R.string.unit_feet)
                secondaryHint = context.getString(R.string.unit_inches)
            } else {
                hint = context.getString(R.string.distance)
            }
        }

    init {
        hint = context.getString(R.string.distance)
        setOnValueChangeListener(null)
    }

    override fun setOnValueChangeListener(listener: ((value: Distance?) -> Unit)?) {
        super.setOnValueChangeListener {
            val wasShowing = showSecondaryAmount
            showSecondaryAmount = showFeetAndInches && unit == DistanceUnits.Feet
            if (!wasShowing && showSecondaryAmount) {
                hint = context.getString(R.string.unit_feet)
                secondaryHint = context.getString(R.string.unit_inches)
            } else if (wasShowing && !showSecondaryAmount) {
                hint = context.getString(R.string.distance)
            } else {
                listener?.invoke(it)
            }
        }
    }

    override fun createDisplayUnit(units: DistanceUnits): MultipartUnitInputView.DisplayUnit<DistanceUnits> {
        return MultipartUnitInputView.DisplayUnit(
            units,
            formatService.getDistanceUnitName(units, true),
            formatService.getDistanceUnitName(units)
        )
    }

    override fun getAmount(value: Distance): Number {
        return if (showSecondaryAmount) {
            floor(value.distance)
        } else {
            value.distance
        }
    }

    override fun getUnit(value: Distance): DistanceUnits {
        return value.units
    }

    override fun map(amount: Number, secondaryAmount: Number?, unit: DistanceUnits): Distance {
        return if (showSecondaryAmount) {
            Distance(amount.toFloat() + (secondaryAmount?.toFloat() ?: 0f) / 12f, unit)
        } else {
            Distance(amount.toFloat(), unit)
        }
    }

    override fun getSecondaryAmount(value: Distance): Number? {
        return if (showSecondaryAmount) {
            val inches = (value.distance % 1f) * 12
            if (inches == 0f) {
                null
            } else {
                inches
            }
        } else {
            null
        }
    }


}