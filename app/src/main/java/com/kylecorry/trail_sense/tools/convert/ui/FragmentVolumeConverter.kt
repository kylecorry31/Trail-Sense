package com.kylecorry.trail_sense.tools.convert.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolVolumeConvertBinding
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trailsensecore.domain.units.*
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment

class FragmentVolumeConverter : BoundFragment<FragmentToolVolumeConvertBinding>() {

    private val formatService by lazy { FormatServiceV2(requireContext()) }

    private val intervalometer = Intervalometer {
        update()
    }

    private val units = VolumeUnits.values()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fromAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_plain,
            R.id.item_name,
            units.map { getUnitName(it) })
        binding.fromUnits.prompt = getString(R.string.distance_from)
        binding.fromUnits.adapter = fromAdapter
        binding.fromUnits.setSelection(0)

        val toAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_plain,
            R.id.item_name,
            units.map { getUnitName(it) })
        binding.toUnits.prompt = getString(R.string.distance_to)
        binding.toUnits.adapter = toAdapter
        binding.toUnits.setSelection(0)

        binding.swapBtn.setOnClickListener {
            val fromCurrent = binding.fromUnits.selectedItemPosition
            val toCurrent = binding.toUnits.selectedItemPosition
            binding.fromUnits.setSelection(toCurrent)
            binding.toUnits.setSelection(fromCurrent)
            update()
        }
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(20)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    fun update() {
        val volume = binding.volumeEdit.text?.toString()?.toFloatOrNull() ?: 0.0f
        val from = units[binding.fromUnits.selectedItemPosition]
        val to = units[binding.toUnits.selectedItemPosition]

        val converted = Volume(volume, from).convertTo(to)

        binding.result.text = formatService.formatVolume(converted, 4, false)
    }

    private fun getUnitName(unit: VolumeUnits): String {
        return when (unit) {
            VolumeUnits.Liters -> getString(R.string.liters)
            VolumeUnits.Milliliter -> getString(R.string.milliliters)
            VolumeUnits.USCups -> getString(R.string.us_cups)
            VolumeUnits.USPints -> getString(R.string.us_pints)
            VolumeUnits.USQuarts -> getString(R.string.us_quarts)
            VolumeUnits.USOunces -> getString(R.string.us_ounces)
            VolumeUnits.USGallons -> getString(R.string.us_gallons)
            VolumeUnits.ImperialCups -> getString(R.string.imperial_cups)
            VolumeUnits.ImperialPints -> getString(R.string.imperial_pints)
            VolumeUnits.ImperialQuarts -> getString(R.string.imperial_quarts)
            VolumeUnits.ImperialOunces -> getString(R.string.imperial_ounces)
            VolumeUnits.ImperialGallons -> getString(R.string.imperial_gallons)
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolVolumeConvertBinding {
        return FragmentToolVolumeConvertBinding.inflate(layoutInflater, container, false)
    }

}