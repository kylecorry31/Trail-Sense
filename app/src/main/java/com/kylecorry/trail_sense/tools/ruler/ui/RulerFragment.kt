package com.kylecorry.trail_sense.tools.ruler.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolRulerBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.isMetric

class RulerFragment : BoundFragment<FragmentToolRulerBinding>() {
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var scaleMode = MapScaleMode.Fractional
    private var currentDistance = Distance(0f, DistanceUnits.Centimeters)

    private lateinit var units: UserPreferences.DistanceUnits

    private var rulerUnits = DistanceUnits.Centimeters

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rulerUnits =
            if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) DistanceUnits.Centimeters else DistanceUnits.Inches
        binding.ruler.metric = rulerUnits.isMetric()
        binding.ruler.setOnTouchListener { distance ->
            binding.ruler.highlight = distance
            onRulerTap(distance)
        }
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
            binding.ruler.metric = rulerUnits.isMetric()
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

        binding.verbalMapScaleFrom.hint = getString(R.string.distance_from)
        binding.verbalMapScaleTo.hint = getString(R.string.distance_to)

        binding.verbalMapScaleFrom.units = formatService.sortDistanceUnits(DistanceUtils.rulerDistanceUnits)

        binding.verbalMapScaleTo.units = formatService.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)

        binding.verbalMapScaleFrom.setOnValueChangeListener {
            calculateMapDistance()
        }

        binding.verbalMapScaleTo.setOnValueChangeListener {
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
        binding.ruler.highlight = null
        units = prefs.distanceUnits
        binding.measurement.text = ""
    }

    private fun onRulerTap(distance: Distance) {
        currentDistance = distance.convertTo(DistanceUnits.Centimeters)
        val displayDistance = currentDistance.convertTo(rulerUnits)
        binding.measurement.text = formatService.formatDistance(displayDistance, precision, false)
        calculateMapDistance()
    }

    private fun calculateMapDistance() {
        val displayDistance = when (scaleMode) {
            MapScaleMode.Relational -> {
                val scaleTo = binding.verbalMapScaleTo.value
                val scaleFrom = binding.verbalMapScaleFrom.value

                if (scaleFrom == null || scaleTo == null) {
                    null
                } else {
                    val mapDistance = Geology.getMapDistance(currentDistance, scaleFrom, scaleTo)
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
                    val mapDistance = Geology.getMapDistance(currentDistance, ratioFrom, ratioTo)
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