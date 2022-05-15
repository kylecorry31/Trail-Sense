package com.kylecorry.trail_sense.tools.light.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.sense.light.LightSensor
import com.kylecorry.sol.science.physics.PhysicsService
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolLightBinding
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.max

class ToolLightFragment : BoundFragment<FragmentToolLightBinding>() {

    private val lightSensor by lazy { LightSensor(requireContext()) }
    private val lightService = PhysicsService()
    private val formatService by lazy { FormatService(requireContext()) }
    private var maxLux = 0f

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolLightBinding {
        return FragmentToolLightBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lightSensor.asLiveData().observe(viewLifecycleOwner) { updateLight() }

        binding.resetBtn.setOnClickListener {
            maxLux = 0f
            updateLight()
        }

        binding.beamDistance.setOnValueChangeListener {
            maxLux = 0f
            if (it != null) {
                binding.lightChart.setDistanceUnits(it.units)
            }
            updateLight()
        }

        binding.beamDistance.units =
            formatService.sortDistanceUnits(listOf(DistanceUnits.Feet, DistanceUnits.Meters))
    }

    private fun updateLight() {
        binding.lightTitle.title.text = formatService.formatLux(lightSensor.illuminance)
        maxLux = max(lightSensor.illuminance, maxLux)

        val distance = binding.beamDistance.value
        if (distance == null) {
            binding.lightTitle.subtitle.text = ""
            binding.beamDistanceText.text = ""
            binding.lightChart.setCandela(0f)
            return
        }

        val candela = lightService.luxToCandela(maxLux, distance)
        val beamDist = lightService.lightBeamDistance(candela).convertTo(distance.units)

        binding.lightTitle.subtitle.text = formatService.formatCandela(candela)
        binding.beamDistanceText.text =
            getString(R.string.beam_distance, formatService.formatDistance(beamDist))
        binding.lightChart.setCandela(candela)
    }
}