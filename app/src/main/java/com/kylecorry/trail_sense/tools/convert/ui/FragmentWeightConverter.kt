package com.kylecorry.trail_sense.tools.convert.ui

import com.kylecorry.andromeda.core.units.Weight
import com.kylecorry.andromeda.core.units.WeightUnits
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.absoluteValue

class FragmentWeightConverter : SimpleConvertFragment<WeightUnits>(WeightUnits.Kilograms, WeightUnits.Pounds) {

    private val formatService by lazy { FormatService(requireContext()) }

    override val units = WeightUnits.values().toList()

    override fun getUnitName(unit: WeightUnits): String {
        return formatService.getWeightUnitName(unit)
    }

    override fun convert(amount: Float, from: WeightUnits, to: WeightUnits): String {
        val converted = Weight(amount.absoluteValue, from).convertTo(to)
        return formatService.formatWeight(converted, 4, false)
    }

}