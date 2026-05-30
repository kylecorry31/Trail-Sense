package com.kylecorry.trail_sense.tools.ruler.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolRulerBinding
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences

class RulerFragment : BoundFragment<FragmentToolRulerBinding>() {
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var scaleMode = MapScaleMode.Fractional
    private var currentDistance = Distance.from(0f, DistanceUnits.Centimeters)

    private lateinit var units: UserPreferences.DistanceUnits

    private var rulerUnits = DistanceUnits.Centimeters

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rulerUnits =
            if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) DistanceUnits.Centimeters else DistanceUnits.Inches
        binding.ruler.metric = rulerUnits.isMetric
        binding.ruler.setOnTouchListener { distance ->
            binding.ruler.highlight = distance
            onRulerTap(distance)
        }
        binding.fractionalMapFrom.setText("1")

        binding.rulerUnitBtn.text = getUnitText(rulerUnits)

        binding.rulerUnitBtn.setOnClickListener {
            rulerUnits = if (rulerUnits == DistanceUnits.Centimeters) {
                DistanceUnits.Inches
            } else {
                DistanceUnits.Centimeters
            }
            binding.rulerUnitBtn.text = getUnitText(rulerUnits)
            val displayDistance = currentDistance.convertTo(rulerUnits)
            binding.measurement.text =
                formatService.formatDistance(displayDistance, precision, false)
            binding.ruler.metric = rulerUnits.isMetric
            calculateMapDistance()
        }

        binding.mapScaleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }

            scaleMode = when (checkedId) {
                R.id.map_ratio_btn -> MapScaleMode.Fractional
                R.id.map_verbal_btn -> MapScaleMode.Relational
                else -> return@addOnButtonCheckedListener
            }

            binding.fractionalMapScale.visibility =
                if (scaleMode == MapScaleMode.Fractional) View.VISIBLE else View.INVISIBLE
            binding.verbalMapScale.visibility =
                if (scaleMode == MapScaleMode.Relational) View.VISIBLE else View.INVISIBLE
            calculateMapDistance()
        }

        binding.verbalMapScaleFrom.hint = getString(R.string.distance_from)
        binding.verbalMapScaleTo.hint = getString(R.string.distance_to)

        binding.verbalMapScaleFrom.units =
            formatService.sortDistanceUnits(DistanceUtils.rulerDistanceUnits)

        binding.verbalMapScaleTo.units =
            formatService.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)

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
                    val mapDistance = Geography.getMapDistance(currentDistance, scaleFrom, scaleTo)
                    formatService.formatDistance(
                        Distance.from(mapDistance.value, scaleTo.units),
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
                    val mapDistance = Geography.getMapDistance(currentDistance, ratioFrom, ratioTo)
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
