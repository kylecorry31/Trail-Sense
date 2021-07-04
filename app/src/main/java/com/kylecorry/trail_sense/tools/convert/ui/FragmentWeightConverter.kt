package com.kylecorry.trail_sense.tools.convert.ui

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trailsensecore.domain.units.*
import kotlin.math.absoluteValue

class FragmentWeightConverter : SimpleConvertFragment<WeightUnits>(WeightUnits.Kilograms, WeightUnits.Pounds) {

    private val formatService by lazy { FormatServiceV2(requireContext()) }

    override val units = WeightUnits.values().toList()

    override fun getUnitName(unit: WeightUnits): String {
        return when (unit) {
            WeightUnits.Pounds -> getString(R.string.pounds)
            WeightUnits.Ounces -> getString(R.string.ounces_weight)
            WeightUnits.Kilograms -> getString(R.string.kilograms)
            WeightUnits.Grams -> getString(R.string.grams)
        }
    }

    override fun convert(amount: Float, from: WeightUnits, to: WeightUnits): String {
        val converted = Weight(amount.absoluteValue, from).convertTo(to)
        return formatService.formatWeight(converted, 4, false)
    }

}