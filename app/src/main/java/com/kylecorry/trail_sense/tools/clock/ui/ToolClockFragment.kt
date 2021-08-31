package com.kylecorry.trail_sense.tools.clock.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.jobs.AlarmTaskScheduler
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolClockBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.clock.infrastructure.NextMinuteBroadcastReceiver
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ToolClockFragment : BoundFragment<FragmentToolClockBinding>() {

    private val formatService by lazy { FormatService(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS(false) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val timer = Timer { update() }

    private var gpsTime = Instant.now()
    private var systemTime = Instant.now()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pipButton.setOnClickListener {
            sendNextMinuteNotification()
        }
        binding.clockRefresh.setOnClickListener {
            gps.start(this::onGPSUpdate)
            binding.updatingClock.visibility = View.VISIBLE
            binding.pipButton.visibility = View.INVISIBLE
        }
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

        if (gps is CustomGPS && (gps as CustomGPS).isTimedOut) {
            Alerts.toast(requireContext(), getString(R.string.no_gps_signal))
            gpsTime = Instant.now()
        }

        binding.updatingClock.visibility = View.INVISIBLE
        binding.pipButton.visibility = View.VISIBLE
        return false
    }

    private fun update() {
        val systemDiff = Duration.between(systemTime, Instant.now())
        val currentTime = gpsTime.plus(systemDiff)
        val myTime = ZonedDateTime.ofInstant(currentTime, ZoneId.systemDefault())
        if (ZoneId.systemDefault() == ZoneId.of("GMT")) {
            binding.utcClock.visibility = View.INVISIBLE
        } else {
            binding.utcClock.visibility = View.VISIBLE
            val utcTime = ZonedDateTime.ofInstant(currentTime, ZoneId.of("UTC"))
            binding.utcClock.text =
                getString(R.string.utc_format, formatService.formatTime(utcTime.toLocalTime()))
        }
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

        Alerts.dialog(
            requireContext(),
            getString(R.string.clock_sync_time_settings),
            getString(R.string.clock_sync_instructions, formattedTime)
        ) { cancelled ->
            if (!cancelled) {
                Alerts.toast(
                    requireContext(),
                    getString(
                        R.string.pip_notification_scheduled,
                        formattedTime
                    )
                )

                val scheduler = AlarmTaskScheduler(requireContext()) {
                    NextMinuteBroadcastReceiver.pendingIntent(
                        requireContext(),
                        formattedTime
                    )
                }
                scheduler.schedule(sendTime.toZonedDateTime().toInstant())
                getResult(Intent(Settings.ACTION_DATE_SETTINGS)) { _, _ ->
                    // Do nothing
                }
            }
        }

    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolClockBinding {
        return FragmentToolClockBinding.inflate(layoutInflater, container, false)
    }
}