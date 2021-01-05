package com.kylecorry.trail_sense.tools.ruler.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolRulerBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits

class RulerFragment : Fragment() {
    private var _binding: FragmentToolRulerBinding? = null
    private val binding get() = _binding!!

    private val formatService by lazy { FormatService(requireContext()) }
    private val geoService = GeoService()
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var scaleMode = MapScaleMode.Fractional
    private var currentDistance = Distance(0f, DistanceUnits.Centimeters)

    private lateinit var ruler: Ruler
    private lateinit var units: UserPreferences.DistanceUnits

    private val unitList = listOf(
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
        _binding = FragmentToolRulerBinding.inflate(inflater, container, false)
        ruler = Ruler(binding.ruler)
        ruler.onTap = this::onRulerTap
        ruler.show()
        binding.fractionalMapFrom.setText("1")

        val fromAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_category,
            R.id.category_name,
            unitList.map { getUnitName(it) })
        binding.verbalFromUnits.prompt = getString(R.string.distance_from)
        binding.verbalFromUnits.adapter = fromAdapter
        binding.verbalFromUnits.setSelection(0)

        val toAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_category,
            R.id.category_name,
            unitList.map { getUnitName(it) })
        binding.verbalToUnits.prompt = getString(R.string.distance_to)
        binding.verbalToUnits.adapter = toAdapter
        binding.verbalToUnits.setSelection(0)

        CustomUiUtils.setButtonState(binding.mapRatioBtn, true)
        CustomUiUtils.setButtonState(binding.mapVerbalBtn, false)

        binding.mapRatioBtn.setOnClickListener {
            scaleMode = MapScaleMode.Fractional
            CustomUiUtils.setButtonState(binding.mapRatioBtn, true)
            CustomUiUtils.setButtonState(binding.mapVerbalBtn, false)
            binding.fractionalMapScale.visibility = View.VISIBLE
            binding.verbalMapScale.visibility = View.INVISIBLE
            calculateMapDistance()
        }

        binding.mapVerbalBtn.setOnClickListener {
            scaleMode = MapScaleMode.Relational
            CustomUiUtils.setButtonState(binding.mapRatioBtn, false)
            CustomUiUtils.setButtonState(binding.mapVerbalBtn, true)
            binding.fractionalMapScale.visibility = View.INVISIBLE
            binding.verbalMapScale.visibility = View.VISIBLE
            calculateMapDistance()
        }

        binding.verbalFromUnits.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                calculateMapDistance()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                calculateMapDistance()
            }
        }

        binding.verbalToUnits.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                calculateMapDistance()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                calculateMapDistance()
            }
        }

        binding.verbalScaleTo.addTextChangedListener {
            calculateMapDistance()
        }

        binding.verbalScaleFrom.addTextChangedListener {
            calculateMapDistance()
        }

        binding.fractionalMapTo.addTextChangedListener {
            calculateMapDistance()
        }
        binding.fractionalMapFrom.addTextChangedListener {
            calculateMapDistance()
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        ruler.clearTap()
        units = prefs.distanceUnits
        binding.measurement.text = ""
    }

    private fun onRulerTap(centimeters: Float) {
        binding.measurement.text = formatService.formatFractionalDistance(centimeters)
        currentDistance = Distance(centimeters, DistanceUnits.Centimeters)
        calculateMapDistance()
    }

    private fun calculateMapDistance() {
        val displayDistance = when (scaleMode) {
            MapScaleMode.Relational -> {
                val scaleFromDist = binding.verbalScaleFrom.text.toString().toFloatOrNull()
                val scaleToDist = binding.verbalScaleTo.text.toString().toFloatOrNull()
                val scaleFrom = Distance(scaleFromDist ?: 0f, unitList[binding.verbalFromUnits.selectedItemPosition])
                val scaleTo = Distance(scaleToDist ?: 0f, unitList[binding.verbalToUnits.selectedItemPosition])

                if (scaleFromDist == null || scaleToDist == null) {
                    null
                } else {
                    val mapDistance = geoService.getMapDistance(currentDistance, scaleFrom, scaleTo)
                    formatService.formatDistance(mapDistance.distance, scaleTo.units)
                }
            }
            MapScaleMode.Fractional -> {
                val ratioFrom: Float? = binding.fractionalMapFrom.text.toString().toFloatOrNull()
                val ratioTo: Float? = binding.fractionalMapTo.text.toString().toFloatOrNull()

                if (ratioFrom == null || ratioTo == null) {
                    null
                } else {
                    val mapDistance = geoService.getMapDistance(currentDistance, ratioFrom, ratioTo)
                        .convertTo(DistanceUnits.Meters)
                    formatService.formatLargeDistance(mapDistance.distance)
                }
            }
        }

        binding.mapDistance.text = if (displayDistance == null) {
            ""
        } else {
            getString(R.string.map_distance, displayDistance)
        }
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


    private enum class MapScaleMode {
        Fractional,
        Relational
    }

}