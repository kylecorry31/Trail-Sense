package com.kylecorry.trail_sense.tools.ruler.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolRulerBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment

class RulerFragment : BoundFragment<FragmentToolRulerBinding>() {
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val geoService = GeoService()
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var scaleMode = MapScaleMode.Fractional
    private var currentDistance = Distance(0f, DistanceUnits.Centimeters)

    private lateinit var ruler: Ruler
    private lateinit var units: UserPreferences.DistanceUnits

    private var rulerUnits = DistanceUnits.Centimeters

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rulerUnits =
            if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) DistanceUnits.Centimeters else DistanceUnits.Inches
        ruler = Ruler(binding.ruler, rulerUnits)
        ruler.onTap = this::onRulerTap
        ruler.show()
        binding.fractionalMapFrom.setText("1")

        CustomUiUtils.setButtonState(binding.mapRatioBtn, true)
        CustomUiUtils.setButtonState(binding.mapVerbalBtn, false)
        CustomUiUtils.setButtonState(binding.rulerUnitBtn, false)
        binding.rulerUnitBtn.text = getUnitText(rulerUnits)

        binding.rulerUnitBtn.setOnClickListener {
            rulerUnits = if (rulerUnits == DistanceUnits.Centimeters) {
                DistanceUnits.Inches
            } else {
                DistanceUnits.Centimeters
            }
            binding.rulerUnitBtn.text = getUnitText(rulerUnits)
            val displayDistance = currentDistance.convertTo(rulerUnits)
            binding.measurement.text = formatService.formatDistance(displayDistance, precision, false)
            ruler.setUnits(rulerUnits)
            calculateMapDistance()
        }

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

        binding.verbalMapScaleFrom.units = listOf(
            DistanceUnits.Centimeters,
            DistanceUnits.Inches
        )

        binding.verbalMapScaleTo.units = listOf(
            DistanceUnits.Kilometers,
            DistanceUnits.Miles,
            DistanceUnits.NauticalMiles,
            DistanceUnits.Meters,
            DistanceUnits.Feet
        )

        binding.verbalMapScaleFrom.setOnDistanceChangeListener {
            calculateMapDistance()
        }

        binding.verbalMapScaleTo.setOnDistanceChangeListener {
            calculateMapDistance()
        }

        binding.fractionalMapTo.addTextChangedListener {
            calculateMapDistance()
        }
        binding.fractionalMapFrom.addTextChangedListener {
            calculateMapDistance()
        }
    }

    override fun onResume() {
        super.onResume()
        ruler.clearTap()
        units = prefs.distanceUnits
        binding.measurement.text = ""
    }

    private fun onRulerTap(centimeters: Float) {
        currentDistance = Distance(centimeters, DistanceUnits.Centimeters)
        val displayDistance = currentDistance.convertTo(rulerUnits)
        binding.measurement.text = formatService.formatDistance(displayDistance, precision, false)
        calculateMapDistance()
    }

    private fun calculateMapDistance() {
        val displayDistance = when (scaleMode) {
            MapScaleMode.Relational -> {
                val scaleTo = binding.verbalMapScaleTo.distance
                val scaleFrom = binding.verbalMapScaleFrom.distance

                if (scaleFrom == null || scaleTo == null) {
                    null
                } else {
                    val mapDistance = geoService.getMapDistance(currentDistance, scaleFrom, scaleTo)
                    formatService.formatDistance(
                        Distance(mapDistance.distance, scaleTo.units),
                        mapPrecision,
                        false
                    )
                }
            }
            MapScaleMode.Fractional -> {
                val ratioFrom: Float? = binding.fractionalMapFrom.text.toString().toFloatOrNull()
                val ratioTo: Float? = binding.fractionalMapTo.text.toString().toFloatOrNull()

                if (ratioFrom == null || ratioTo == null) {
                    null
                } else {
                    val mapDistance = geoService.getMapDistance(currentDistance, ratioFrom, ratioTo)
                    formatService.formatDistance(
                        mapDistance.convertTo(rulerUnits).toRelativeDistance(), mapPrecision, false
                    )
                }
            }
        }

        binding.mapDistance.text = if (displayDistance == null) {
            ""
        } else {
            getString(R.string.map_distance, displayDistance)
        }
    }

    private fun getUnitText(units: DistanceUnits): String {
        return if (units == DistanceUnits.Centimeters) {
            getString(R.string.unit_centimeters_abbreviation)
        } else {
            getString(R.string.unit_inches_abbreviation)
        }
    }


    private enum class MapScaleMode {
        Fractional,
        Relational
    }

    companion object {
        const val precision = 4
        const val mapPrecision = 2
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolRulerBinding {
        return FragmentToolRulerBinding.inflate(layoutInflater, container, false)
    }

}