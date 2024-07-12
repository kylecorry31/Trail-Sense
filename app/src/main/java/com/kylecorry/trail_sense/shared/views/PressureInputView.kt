package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

class PressureInputView(context: Context, attributeSet: AttributeSet? = null) :
    BaseUnitInputView<Pressure, PressureUnits>(context, attributeSet) {

    private val formatService by lazy { FormatService.getInstance(context) }

    init {
        hint = context.getString(R.string.pressure)
    }

    override fun createDisplayUnit(units: PressureUnits): UnitInputView.DisplayUnit<PressureUnits> {
        // TODO: Support long name
        return UnitInputView.DisplayUnit(
            units,
            formatService.getPressureUnitName(units),
            formatService.getPressureUnitName(units)
        )
    }

    override fun map(amount: Number, unit: PressureUnits): Pressure {
        return Pressure(amount.toFloat(), unit)
    }

    override fun getAmount(value: Pressure): Number {
        return value.pressure
    }

    override fun getUnit(value: Pressure): PressureUnits {
        return value.units
    }

}