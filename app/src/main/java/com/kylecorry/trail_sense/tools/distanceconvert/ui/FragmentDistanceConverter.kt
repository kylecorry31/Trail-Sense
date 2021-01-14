package com.kylecorry.trail_sense.tools.distanceconvert.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolDistanceConvertBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.UnitService
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class FragmentDistanceConverter : Fragment() {

    private var _binding: FragmentToolDistanceConvertBinding? = null
    private val binding get() = _binding!!

    private val unitService = UnitService()
    private val formatService by lazy { FormatService(requireContext()) }

    private val intervalometer = Intervalometer {
        update()
    }

    private val units = listOf(
        DistanceUnits.Centimeters,
        DistanceUnits.Meters,
        DistanceUnits.Kilometers,
        DistanceUnits.Inches,
        DistanceUnits.Feet,
        DistanceUnits.Miles,
        DistanceUnits.NauticalMiles,
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolDistanceConvertBinding.inflate(inflater, container, false)

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

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(20)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    fun update(){
        val distance = binding.distanceEdit.text?.toString()?.toFloatOrNull() ?: 0.0f
        val from = units[binding.fromUnits.selectedItemPosition]
        val to = units[binding.toUnits.selectedItemPosition]

        val converted = unitService.convert(distance, from, to)

        binding.result.text = formatService.formatDistancePrecise(converted, to)
    }

    private fun getUnitName(unit: DistanceUnits): String {
        return when(unit){
            DistanceUnits.Meters -> getString(R.string.unit_meters)
            DistanceUnits.Kilometers -> getString(R.string.unit_kilometers)
            DistanceUnits.Feet -> getString(R.string.unit_feet)
            DistanceUnits.Miles -> getString(R.string.unit_miles)
            DistanceUnits.NauticalMiles -> getString(R.string.unit_nautical_miles)
            DistanceUnits.Centimeters -> getString(R.string.unit_centimeters)
            DistanceUnits.Inches -> getString(R.string.unit_inches)
        }
    }

}