package com.kylecorry.trail_sense.tools.light.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolLightBinding
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.light.LightService
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.infrastructure.sensors.asLiveData
import com.kylecorry.trailsensecore.infrastructure.sensors.light.LightSensor
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import kotlin.math.max

class ToolLightFragment : BoundFragment<FragmentToolLightBinding>() {

    private val lightSensor by lazy { LightSensor(requireContext()) }
    private val lightService = LightService()
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private var maxLux = 0f

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolLightBinding {
        return FragmentToolLightBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lightSensor.asLiveData().observe(viewLifecycleOwner, { updateLight() })

        binding.resetBtn.setOnClickListener {
            maxLux = 0f
            updateLight()
        }

        binding.beamDistance.setOnDistanceChangeListener {
            maxLux = 0f
            if (it != null) {
                binding.lightChart.setDistanceUnits(it.units)
            }
            updateLight()
        }

        binding.beamDistance.units =
            if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) {
                listOf(
                    DistanceUnits.Meters,
                    DistanceUnits.Feet
                )
            } else {
                listOf(
                    DistanceUnits.Feet,
                    DistanceUnits.Meters
                )
            }
    }

    private fun updateLight() {
        binding.lux.text = formatService.formatLux(lightSensor.illuminance)
        maxLux = max(lightSensor.illuminance, maxLux)

        val distance = binding.beamDistance.distance
        if (distance == null) {
            binding.intensity.text = ""
            binding.beamDistanceText.text = ""
            binding.lightChart.setCandela(0f)
            return
        }

        val candela = lightService.toCandela(maxLux, distance)
        val beamDist = lightService.beamDistance(candela).convertTo(distance.units)

        binding.intensity.text = formatService.formatCandela(candela)
        binding.beamDistanceText.text =
            getString(R.string.beam_distance, formatService.formatDistance(beamDist))
        binding.lightChart.setCandela(candela)
    }
}