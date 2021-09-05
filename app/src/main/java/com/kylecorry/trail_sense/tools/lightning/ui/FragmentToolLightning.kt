package com.kylecorry.trail_sense.tools.lightning.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.meteorology.WeatherService
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolLightningBinding
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Instant

class FragmentToolLightning : BoundFragment<FragmentToolLightningBinding>() {
    private val weatherService = WeatherService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private lateinit var units: DistanceUnits

    private var lightningTime: Instant? = null
    private var distance: Distance? = null

    private val intervalometer = Timer {
        val lightning = lightningTime
        if (lightning != null) {
            val d =
                Distance.meters(weatherService.getLightningStrikeDistance(lightning, Instant.now()))
                    .convertTo(units)
                    .toRelativeDistance()
            binding.strikeDistance.text = formatService.formatDistance(
                d, Units.getDecimalPlaces(d.units),
                false
            )
            binding.strikeClose.isVisible = weatherService.isLightningStrikeDangerous(d)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startBtn.setOnClickListener {
            val lightning = lightningTime
            if (lightning == null) {
                lightningTime = Instant.now()
                distance = null
                binding.startBtn.setImageResource(R.drawable.ic_thunder)
                binding.startBtn.setText(getString(R.string.thunder))
                binding.startBtn.setState(true)
            } else {
                lightningTime = null
                binding.startBtn.setImageResource(R.drawable.ic_lightning)
                binding.startBtn.setText(getString(R.string.lightning))
                binding.startBtn.setState(false)
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolLightningBinding {
        return FragmentToolLightningBinding.inflate(layoutInflater, container, false)
    }

    override fun onResume() {
        super.onResume()
        units = prefs.baseDistanceUnits
        distance = null
        lightningTime = null
        binding.strikeDistance.text = ""
        binding.startBtn.setImageResource(R.drawable.ic_lightning)
        binding.startBtn.setText(getString(R.string.lightning))
        binding.startBtn.setState(false)
        intervalometer.interval(20)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

}