package com.kylecorry.trail_sense.tools.clock.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolClockBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.clock.infrastructure.NextMinuteBroadcastReceiver
import com.kylecorry.trailsensecore.infrastructure.system.AlarmUtils
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.*
import java.time.temporal.ChronoUnit

class ToolClockFragment : Fragment() {
    private var _binding: FragmentToolClockBinding? = null
    private val binding get() = _binding!!

    private val formatService by lazy { FormatService(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS(false) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val timer = Intervalometer { update() }

    private var gpsTime = Instant.now()
    private var systemTime = Instant.now()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolClockBinding.inflate(inflater, container, false)
        binding.pipButton.setOnClickListener {
            sendNextMinuteNotification()
        }
        binding.clockRefresh.setOnClickListener {
            gps.start(this::onGPSUpdate)
            binding.updatingClock.visibility = View.VISIBLE
            binding.pipButton.visibility = View.INVISIBLE
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        gps.start(this::onGPSUpdate)
        binding.updatingClock.visibility = View.VISIBLE
        binding.pipButton.visibility = View.INVISIBLE
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

        if (gps is CustomGPS && (gps as CustomGPS).isTimedOut){
            UiUtils.shortToast(requireContext(), getString(R.string.no_gps_signal))
            gpsTime = Instant.now()
        }

        binding.updatingClock.visibility = View.INVISIBLE
        binding.pipButton.visibility = View.VISIBLE
        return false
    }

    private fun update() {
        val systemDiff = Duration.between(systemTime, Instant.now())
        val currentTime = gpsTime.plus(systemDiff)
        val utcTime = ZonedDateTime.ofInstant(currentTime, ZoneId.of("UTC"))
        val myTime = ZonedDateTime.ofInstant(currentTime, ZoneId.systemDefault())
        binding.utcClock.text =
            getString(R.string.utc_format, formatService.formatTime(utcTime.toLocalTime()))
        binding.clock.text = formatService.formatTime(myTime.toLocalTime())
        binding.date.text = formatService.formatDate(myTime)
        binding.analogClock.time = myTime.toLocalTime()
        binding.analogClock.use24Hours = prefs.use24HourTime
    }

    private fun sendNextMinuteNotification() {
        val systemDiff = Duration.between(systemTime, Instant.now())
        val currentTime = gpsTime.plus(systemDiff)
        val clockError = Duration.between(systemTime, gpsTime)
        val myTime = ZonedDateTime.ofInstant(currentTime, ZoneId.systemDefault())
        val displayTimeWithSeconds = myTime.toLocalDateTime()
        var displayTime = displayTimeWithSeconds.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
        val displayTimeSecond = displayTimeWithSeconds.second
        if (displayTimeSecond > 45) {
            displayTime = displayTimeWithSeconds.truncatedTo(ChronoUnit.MINUTES).plusMinutes(2)
        }
        val sendTime = displayTime.minus(clockError)

        val formattedTime = formatService.formatTime(displayTime.toLocalTime())

        UiUtils.alertWithCancel(
            requireContext(),
            getString(R.string.clock_sync_time_settings),
            getString(R.string.clock_sync_instructions, formattedTime)
        ) { cancelled ->
            if (!cancelled) {
                UiUtils.shortToast(
                    requireContext(),
                    getString(
                        R.string.pip_notification_scheduled,
                        formattedTime
                    )
                )

                AlarmUtils.set(
                    requireContext(),
                    sendTime,
                    NextMinuteBroadcastReceiver.pendingIntent(
                        requireContext(),
                        formattedTime
                    ),
                    exact = true,
                    allowWhileIdle = true
                )
                startActivityForResult(Intent(Settings.ACTION_DATE_SETTINGS), 0)
            }
        }

    }
}