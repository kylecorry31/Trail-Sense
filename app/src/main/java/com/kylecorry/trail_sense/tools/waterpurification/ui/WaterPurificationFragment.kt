package com.kylecorry.trail_sense.tools.waterpurification.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.sensors.read
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolWaterPurificationBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.waterpurification.domain.WaterService
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant

class WaterPurificationFragment : BoundFragment<FragmentToolWaterPurificationBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val altimeter by lazy { sensorService.getAltimeter(false) }
    private val cache by lazy { Preferences(requireContext()) }
    private var duration: Duration? = null
    private val waterService = WaterService()
    private var selectedTime = TimeSelection.Auto
    private var updateJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.boilButton.setOnClickListener {
            if (isRunning()) {
                stop()
            } else {
                start()
            }
        }

        binding.chipAuto.setOnClickListener {
            selectedTime = TimeSelection.Auto
            updateSelectedDuration()
        }

        binding.chip1Min.setOnClickListener {
            selectedTime = TimeSelection.LowAltitude
            updateSelectedDuration()
        }

        binding.chip3Min.setOnClickListener {
            selectedTime = TimeSelection.HighAltitude
            updateSelectedDuration()
        }

        binding.chip1Min.text = formatService.formatDuration(Duration.ofMinutes(1), short = true)
        binding.chip3Min.text = formatService.formatDuration(Duration.ofMinutes(3), short = true)

        scheduleUpdates(INTERVAL_30_FPS)
    }

    override fun onResume() {
        super.onResume()
        updateSelectedDuration()
    }

    override fun onUpdate() {
        super.onUpdate()
        val remaining = getRemainingTime() ?: duration
        if (remaining != null) {
            binding.timeLeft.text = remaining.seconds.toString()
            binding.boilLoading.isVisible = false
            binding.timeLeft.isVisible = true
            binding.boilButton.isVisible = true
        } else {
            binding.boilLoading.isVisible = true
            binding.timeLeft.isVisible = false
            binding.boilButton.isVisible = false
        }

        if (isRunning()) {
            binding.boilButton.text = getString(android.R.string.cancel)
            binding.timeChips.isVisible = false
        } else {
            binding.boilButton.text = getString(R.string.boil_start)
            binding.timeChips.isVisible = true
        }

        CustomUiUtils.setButtonState(binding.chipAuto, selectedTime == TimeSelection.Auto)
        CustomUiUtils.setButtonState(binding.chip1Min, selectedTime == TimeSelection.LowAltitude)
        CustomUiUtils.setButtonState(binding.chip3Min, selectedTime == TimeSelection.HighAltitude)
    }

    private fun start() {
        stop()
        val duration = duration ?: return
        cache.putInstant(
            WATER_PURIFICATION_END_TIME_KEY,
            Instant.now().plus(duration)
        )
        WaterPurificationTimerService.start(requireContext(), duration.seconds)
    }

    private fun stop() {
        cache.remove(WATER_PURIFICATION_END_TIME_KEY)
        WaterPurificationTimerService.stop(requireContext())
        updateSelectedDuration()
    }

    private fun updateSelectedDuration() {
        duration = null
        updateJob?.cancel()
        updateJob = lifecycleScope.launch {
            val duration = getSelectedDuration()

            withContext(Dispatchers.Main) {
                setBoilTime(duration)
            }
        }
    }

    private suspend fun getSelectedDuration(): Duration {

        if (selectedTime == TimeSelection.Auto) {
            // Try to update the altimeter
            if (!altimeter.hasValidReading) {
                withTimeoutOrNull(10000) {
                    altimeter.read()
                }
            }

            // Only use altimeter if there's a valid reading
            if (altimeter.hasValidReading) {
                return waterService.getPurificationTime(
                    Distance(altimeter.altitude, DistanceUnits.Meters)
                )
            }
        } else if (selectedTime == TimeSelection.LowAltitude) {
            return Duration.ofMinutes(1)
        }

        return Duration.ofMinutes(3)
    }

    private fun setBoilTime(time: Duration) {
        duration = time
    }

    private fun isRunning(): Boolean {
        return getRemainingTime() != null
    }

    private fun getRemainingTime(): Duration? {
        val lastEndTime =
            cache.getLong(WATER_PURIFICATION_END_TIME_KEY) ?: return null
        val oldDuration = Duration.between(Instant.now(), Instant.ofEpochMilli(lastEndTime))
        return if (!oldDuration.isNegative && !oldDuration.isZero) {
            oldDuration
        } else {
            null
        }
    }

    companion object {
        const val WATER_PURIFICATION_END_TIME_KEY = "water_purification_start_time"
    }

    private enum class TimeSelection {
        Auto,
        LowAltitude,
        HighAltitude
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolWaterPurificationBinding {
        return FragmentToolWaterPurificationBinding.inflate(layoutInflater, container, false)
    }

}