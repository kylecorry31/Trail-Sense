package com.kylecorry.trail_sense.tools.convert.ui

import com.kylecorry.sol.units.Volume
import com.kylecorry.sol.units.VolumeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.absoluteValue

class FragmentVolumeConverter : SimpleConvertFragment<VolumeUnits>(VolumeUnits.Liters, VolumeUnits.USGallons) {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    override val units = VolumeUnits.values().toList()

    override fun getUnitName(unit: VolumeUnits): String {
        return when (unit) {
            VolumeUnits.Liters -> getString(R.string.liters)
            VolumeUnits.Milliliter -> getString(R.string.milliliters)
            VolumeUnits.USCups -> getString(R.string.us_cups)
            VolumeUnits.USPints -> getString(R.string.us_pints)
            VolumeUnits.USQuarts -> getString(R.string.us_quarts)
            VolumeUnits.USOunces -> getString(R.string.us_ounces_volume)
            VolumeUnits.USGallons -> getString(R.string.us_gallons)
            VolumeUnits.ImperialCups -> getString(R.string.imperial_cups)
            VolumeUnits.ImperialPints -> getString(R.string.imperial_pints)
            VolumeUnits.ImperialQuarts -> getString(R.string.imperial_quarts)
            VolumeUnits.ImperialOunces -> getString(R.string.imperial_ounces_volume)
            VolumeUnits.ImperialGallons -> getString(R.string.imperial_gallons)
        }
    }

    override fun convert(amount: Float, from: VolumeUnits, to: VolumeUnits): String {
        val converted = Volume(amount.absoluteValue, from).convertTo(to)
        return formatService.formatVolume(converted, 4, false)
    }

}