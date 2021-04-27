package com.kylecorry.trail_sense.tools.lightning.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolLightningBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.weather.WeatherService
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import java.time.Instant

class FragmentToolLightning : BoundFragment<FragmentToolLightningBinding>() {
    private val weatherService = WeatherService()
    private val formatService by lazy { FormatService(requireContext()) }

    private var lightningTime: Instant? = null
    private var distance: Float? = null
    
    private val intervalometer = Intervalometer {
        val lightning = lightningTime
        if (lightning != null){
            val d = weatherService.getLightningStrikeDistance(lightning, Instant.now())
            binding.strikeDistance.text = formatService.formatLargeDistance(d)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startBtn.setOnClickListener {
            val lightning = lightningTime
            if (lightning == null){
                lightningTime = Instant.now()
                distance = null
                binding.startBtn.setImageResource(R.drawable.ic_thunder)
                binding.startBtn.setText(getString(R.string.thunder))
                binding.startBtn.setState(true)
            } else if (distance == null){
                val d = weatherService.getLightningStrikeDistance(lightning, Instant.now())
                distance = d
                binding.strikeDistance.text = formatService.formatLargeDistance(d)
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