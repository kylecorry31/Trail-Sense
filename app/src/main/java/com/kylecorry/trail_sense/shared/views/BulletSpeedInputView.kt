package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

class BulletSpeedInputView(context: Context, attributeSet: AttributeSet? = null) :
    BaseUnitInputView<Speed, DistanceUnits>(context, attributeSet) {

    private val formatter = AppServiceRegistry.get<FormatService>()

    init {
        hint = context.getString(R.string.speed)
    }

    override fun createDisplayUnit(units: DistanceUnits): UnitInputView.DisplayUnit<DistanceUnits> {
        return UnitInputView.DisplayUnit(
            units,
            formatter.getBulletSpeedUnitName(units, true),
            formatter.getBulletSpeedUnitName(units)
        )
    }

    override fun map(amount: Number, unit: DistanceUnits): Speed {
        return Speed(amount.toFloat(), unit, TimeUnits.Seconds)
    }

    override fun getAmount(value: Speed): Number {
        return value.speed
    }

    override fun getUnit(value: Speed): DistanceUnits {
        return value.distanceUnits
    }

}