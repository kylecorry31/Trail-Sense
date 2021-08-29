package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.forms.UnitInputView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

class DistanceInputView(context: Context, attrs: AttributeSet? = null) :
    BaseUnitInputView<Distance, DistanceUnits>(context, attrs) {

    private val formatService by lazy { FormatService(context) }

    init {
        hint = context.getString(R.string.distance)
    }

    override fun createDisplayUnit(units: DistanceUnits): UnitInputView.DisplayUnit<DistanceUnits> {
        return UnitInputView.DisplayUnit(
            units,
            formatService.getDistanceUnitName(units, true),
            formatService.getDistanceUnitName(units)
        )
    }

    override fun map(amount: Number, unit: DistanceUnits): Distance {
        return Distance(amount.toFloat(), unit)
    }

    override fun getAmount(value: Distance): Number {
        return value.distance
    }

    override fun getUnit(value: Distance): DistanceUnits {
        return value.units
    }


}