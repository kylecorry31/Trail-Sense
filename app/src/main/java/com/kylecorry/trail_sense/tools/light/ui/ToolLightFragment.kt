package com.kylecorry.trail_sense.tools.light.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.trail_sense.databinding.FragmentToolLightBinding
import com.kylecorry.trail_sense.shared.BoundFragment
import com.kylecorry.trail_sense.tools.light.domain.LightService
import com.kylecorry.trail_sense.tools.light.infrastructure.LightSensor
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.infrastructure.sensors.asLiveData
import kotlin.math.max
import kotlin.math.roundToInt

class ToolLightFragment : BoundFragment<FragmentToolLightBinding>() {

    private val lightSensor by lazy { LightSensor(requireContext()) }
    private val lightService = LightService()
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
            updateLight()
        }
    }

    private fun updateLight() {
        val distance = binding.beamDistance.distance ?: Distance(1f, DistanceUnits.Meters)
        maxLux = max(lightSensor.illuminance, maxLux)
        val candela = lightService.toCandela(maxLux, distance)
        val beamDist = lightService.beamDistance(candela)
        binding.candela.text = candela.roundToInt().toString()
        binding.intensity.text =
            lightService.describeLux(lightSensor.illuminance).name + "\n" + beamDist.distance.roundToInt()
                .toString()

        binding.lightChart.setCandela(candela)
    }
}