package com.kylecorry.trail_sense.tools.lightning.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.databinding.FragmentToolLightningBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.weather.WeatherService
import java.time.Instant

class FragmentToolLightning : Fragment() {
    private var _binding: FragmentToolLightningBinding? = null
    private val binding get() = _binding!!

    private val weatherService = WeatherService()
    private val formatService by lazy { FormatService(requireContext()) }

    private var lightningTime: Instant? = null
    private var distance: Float? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolLightningBinding.inflate(inflater, container, false)
        binding.lightningBtn.setOnClickListener {
            onLightning()
        }
        binding.thunderBtn.setOnClickListener {
            onThunder()
        }
        binding.resetBtn.setOnClickListener {
            reset()
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        reset()
    }

    private fun reset() {
        lightningTime = null
        distance = null
        binding.lightningBtn.setState(false)
        binding.thunderBtn.setState(false)
        binding.lightningDistance.text = ""
        binding.resetBtn.visibility = View.INVISIBLE
    }

    private fun onLightning() {
        if (lightningTime != null) {
            if (distance != null) {
                reset()
            }
            else {
                return
            }
        }
        lightningTime = Instant.now()
        binding.lightningBtn.setState(true)
        binding.resetBtn.visibility = View.VISIBLE
    }

    private fun onThunder() {
        if (distance != null) {
            return
        }
        val lightning = lightningTime ?: return
        distance = weatherService.getLightningStrikeDistance(lightning, Instant.now())
        binding.lightningDistance.text = formatService.formatLargeDistance(distance!!)
        binding.thunderBtn.setState(true)
    }

}