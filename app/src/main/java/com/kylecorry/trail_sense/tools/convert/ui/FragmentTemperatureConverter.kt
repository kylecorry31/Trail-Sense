package com.kylecorry.trail_sense.tools.convert.ui

import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

class FragmentTemperatureConverter :
    SimpleConvertFragment<TemperatureUnits>(TemperatureUnits.C, TemperatureUnits.F) {

    private val formatService by lazy { FormatService(requireContext()) }

    override fun getUnitName(unit: TemperatureUnits): String {
        return when (unit) {
            TemperatureUnits.F -> getString(R.string.fahrenheit)
            TemperatureUnits.C -> getString(R.string.celsius)
        }
    }

    override val units: List<TemperatureUnits> = TemperatureUnits.values().toList()

    override fun convert(amount: Float, from: TemperatureUnits, to: TemperatureUnits): String {
        val converted = Temperature(amount, from).convertTo(to)
        return formatService.formatTemperature(converted, 4, false)
    }

}