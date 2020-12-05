package com.kylecorry.trail_sense.tools.clock.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolClockBinding
import com.kylecorry.trail_sense.databinding.FragmentToolDepthBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.*

class ToolClockFragment : Fragment() {
    private var _binding: FragmentToolClockBinding? = null
    private val binding get() = _binding!!

    private val formatService by lazy { FormatService(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS(false) }
    private val timer = Intervalometer { update() }

    private var gpsTime = Instant.now()
    private var systemTime = Instant.now()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolClockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        gps.start(this::onGPSUpdate)
        timer.interval(20)
    }

    override fun onPause() {
        super.onPause()
        gps.stop(this::onGPSUpdate)
        timer.stop()
    }

    private fun onGPSUpdate(): Boolean {
        gpsTime = gps.time
        systemTime = Instant.now()
        UiUtils.shortToast(requireContext(), getString(R.string.gps_time_toast))
        return false
    }

    private fun update(){
        val systemDiff = Duration.between(systemTime, Instant.now())
        val currentTime = gpsTime.plus(systemDiff)
        val utcTime = ZonedDateTime.ofInstant(currentTime, ZoneId.of("UTC"))
        val myTime = ZonedDateTime.ofInstant(currentTime, ZoneId.systemDefault())
        binding.utcClock.text = getString(R.string.utc_format, formatService.formatTime(utcTime.toLocalTime()))
        binding.clock.text = formatService.formatTime(myTime.toLocalTime())
        binding.date.text = formatService.formatDate(myTime)
    }
}