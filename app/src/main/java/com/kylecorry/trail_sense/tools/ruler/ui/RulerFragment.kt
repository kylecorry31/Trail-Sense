package com.kylecorry.trail_sense.tools.ruler.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolRulerBinding
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolRulerBinding.inflate(inflater, container, false)
        ruler = Ruler(binding.ruler)
        ruler.onTap = this::onRulerTap
        ruler.show()
        binding.fractionalMapFrom.setText("1")
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
                val scaleFrom: Distance? = Distance(1f, DistanceUnits.Centimeters)
                val scaleTo: Distance? = Distance(1f, DistanceUnits.Kilometers)

                if (scaleFrom == null || scaleTo == null) {
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


    private enum class MapScaleMode {
        Fractional,
        Relational
    }

}