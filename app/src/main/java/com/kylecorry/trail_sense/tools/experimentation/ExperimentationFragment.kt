package com.kylecorry.trail_sense.tools.experimentation

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.science.astronomy.stars.StarAltitudeReading
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.databinding.FragmentExperimentationBinding
import com.kylecorry.trail_sense.shared.formatEnumName
import java.time.ZonedDateTime

class ExperimentationFragment : BoundFragment<FragmentExperimentationBinding>() {

    private var stars by state(listOf<StarAltitudeReading>())
    private var location by state<Coordinate?>(null)

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentExperimentationBinding {
        return FragmentExperimentationBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.camera.setScaleType(PreviewView.ScaleType.FILL_CENTER)
        binding.camera.setShowTorch(false)
        binding.camera.setExposureCompensation(1f)
        binding.arView.bind(binding.camera)
        binding.arView.backgroundFillColor = Color.TRANSPARENT
        binding.arView.inclinationDecimalPlaces = 2
        binding.arView.reticleDiameter = Resources.dp(requireContext(), 8f)

        binding.recordBtn.setOnClickListener {
            val inclination = binding.arView.inclination
            inBackground {
                val allStars = Star.entries.sortedBy { it.name }
                val starIdx = CoroutinePickers.item(
                    requireContext(),
                    "Star",
                    allStars.map { formatEnumName(it.name) })
                if (starIdx != null) {
                    stars = stars + StarAltitudeReading(
                        allStars[starIdx],
                        inclination,
                        ZonedDateTime.now()
                    )
                    // TODO: Pass in the last known location (let user choose whether to do this or not)
                    // TODO: Let the user pick their last known location on a map
                    location = onDefault { Astronomy.getLocationFromStars(stars) }
                }
            }
        }
    }


    override fun onUpdate() {
        super.onUpdate()
        effect2(stars) {
            binding.text.text = stars.joinToString("\n") { "${it.star.name}: ${it.altitude}" }
        }

        effect2(location) {
            binding.location.text = location?.toString() ?: "Unknown"
        }
    }

    override fun onResume() {
        super.onResume()
        binding.arView.start()
        binding.camera.start(
            readFrames = false,
            shouldStabilizePreview = false
        )
    }

    override fun onPause() {
        super.onPause()
        binding.arView.stop()
        binding.camera.stop()
    }

}