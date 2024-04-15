package com.kylecorry.trail_sense.tools.clock.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.background.AlarmTaskScheduler
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolClockBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.requestScheduleExactAlarms
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.clock.infrastructure.NextMinuteBroadcastReceiver
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ToolClockFragment : BoundFragment<FragmentToolClockBinding>() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val timer = CoroutineTimer { update() }

    private var gpsTime = Instant.now()
    private var systemTime = Instant.now()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pipButton.setOnClickListener {
            requestScheduleExactAlarms {
                sendNextMinuteNotification()
            }
        }
        CustomUiUtils.setButtonState(binding.clockTitle.rightButton, false)
        binding.clockTitle.rightButton.setOnClickListener {
            gps.start(this::onGPSUpdate)
            binding.updatingClock.visibility = View.VISIBLE
            binding.pipButton.visibility = View.INVISIBLE
        }

        val showAnalogClock = prefs.clock.showAnalogClock
        binding.clockTitle.title.isVisible = showAnalogClock
        binding.analogClock.isVisible = showAnalogClock
        binding.digitalClock.isVisible = !showAnalogClock
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
        gpsTime = getGPSTime()
        systemTime = Instant.now()

        if (gps is CustomGPS && (gps as CustomGPS).isTimedOut) {
            Alerts.toast(requireContext(), getString(R.string.no_gps_signal))
            gpsTime = Instant.now()
        }

        binding.updatingClock.visibility = View.INVISIBLE
        binding.pipButton.visibility = View.VISIBLE
        return false
    }

    private fun getGPSTime(): Instant {
        // First try to use the system API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tryOrNothing {
                return SystemClock.currentGnssTimeClock().instant()
            }
        }

        // That didn't work, so get the time from the GPS
        val fixTime = gps.fixTimeElapsedNanos ?: return gps.time
        val systemTime = SystemClock.elapsedRealtimeNanos()
        val timeDiff = fixTime - systemTime
        return gps.time.plusNanos(timeDiff)
    }

    private fun update() {
        val systemDiff = Duration.between(systemTime, Instant.now())
        val currentTime = gpsTime.plus(systemDiff)
        val myTime = ZonedDateTime.ofInstant(currentTime, ZoneId.systemDefault())
        binding.clockTitle.subtitle.text = formatService.formatDate(myTime)
        if (prefs.clock.showAnalogClock) {
            binding.clockTitle.title.text = formatService.formatTime(myTime.toLocalTime())
            binding.analogClock.time = myTime.toLocalTime()
            binding.analogClock.use24Hours = prefs.use24HourTime
        } else {
            binding.digitalClock.text = formatService.formatTime(myTime.toLocalTime())
        }

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
                scheduler.once(sendTime.toZonedDateTime().toInstant())
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