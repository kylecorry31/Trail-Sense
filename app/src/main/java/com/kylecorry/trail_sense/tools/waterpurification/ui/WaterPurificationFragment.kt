package com.kylecorry.trail_sense.tools.waterpurification.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolWaterPurificationBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.waterpurification.domain.WaterService
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant

class WaterPurificationFragment : BoundFragment<FragmentToolWaterPurificationBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val altimeter by lazy { sensorService.getAltimeter(false) }
    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private var duration: Duration? = null
    private val waterService = WaterService()
    private var selectedTime = TimeSelection.Auto
    private val runner = ControlledRunner<Unit>()

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
            binding.boilButton.text = getString(R.string.start)
            binding.timeChips.isVisible = true
        }

        CustomUiUtils.setButtonState(binding.chipAuto, selectedTime == TimeSelection.Auto)
        CustomUiUtils.setButtonState(binding.chip1Min, selectedTime == TimeSelection.LowAltitude)
        CustomUiUtils.setButtonState(binding.chip3Min, selectedTime == TimeSelection.HighAltitude)
    }

    private fun start() {
        stop(updateUI = false)
        inBackground {
            val duration = duration ?: getSelectedDuration()
            onIO {
                cache.putInstant(
                    WATER_PURIFICATION_END_TIME_KEY,
                    Instant.now().plus(duration)
                )
            }
            onMain {
                WaterPurificationTimerService.start(requireContext(), duration.seconds)
            }
        }
    }

    private fun stop(updateUI: Boolean = true) {
        cache.remove(WATER_PURIFICATION_END_TIME_KEY)
        WaterPurificationTimerService.stop(requireContext())
        if (updateUI) {
            updateSelectedDuration()
        }
    }

    private fun updateSelectedDuration() {
        duration = null
        inBackground {
            runner.cancelPreviousThenRun {
                val duration = getSelectedDuration()

                onMain {
                    setBoilTime(duration)
                }
            }
        }
    }

    private suspend fun getSelectedDuration(): Duration = onDefault {
        if (selectedTime == TimeSelection.Auto) {
            // Try to update the altimeter
            if (!altimeter.hasValidReading) {
                withTimeoutOrNull(Duration.ofSeconds(10).toMillis()) {
                    altimeter.read()
                }
            }

            // Only use altimeter if there's a valid reading
            if (altimeter.hasValidReading) {
                return@onDefault waterService.getPurificationTime(
                    Distance(altimeter.altitude, DistanceUnits.Meters)
                )
            }
        } else if (selectedTime == TimeSelection.LowAltitude) {
            return@onDefault Duration.ofMinutes(1)
        }

        Duration.ofMinutes(3)
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