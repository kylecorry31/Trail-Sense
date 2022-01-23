package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolPedometerBinding
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class FragmentToolPedometer : BoundFragment<FragmentToolPedometerBinding>() {

    private val counter by lazy { StepCounter(Preferences(requireContext())) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolPedometerBinding {
        return FragmentToolPedometerBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.resetBtn.setOnClickListener {
            counter.reset()
        }
        scheduleUpdates(20)
    }

    override fun onUpdate() {
        super.onUpdate()
        val distance = getDistance(counter.steps)
        val lastReset = counter.startTime?.toZonedDateTime()

        if (lastReset != null) {
            val dateString = if (lastReset.toLocalDate() == LocalDate.now()) {
                formatService.formatTime(lastReset.toLocalTime(), false)
            } else {
                formatService.formatRelativeDate(lastReset.toLocalDate())
            }
            binding.pedometerTitle.subtitle.text = getString(R.string.since_time, dateString)

            val speed = getSpeed(lastReset.toInstant(), distance)
            binding.pedometerSpeed.title = if (speed != null) {
                formatService.formatSpeed(speed.speed)
            } else {
                getString(R.string.dash)
            }
        }

        binding.pedometerSteps.title = DecimalFormatter.format(counter.steps, 0)

        binding.pedometerTitle.subtitle.isVisible = lastReset != null

        binding.pedometerTitle.title.text = formatService.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )
    }

    private fun getDistance(steps: Long): Distance {
        // TODO: Move this into a service class
        val stride = prefs.strideLength.meters().distance
        val units = prefs.baseDistanceUnits
        return Distance.meters(steps * stride).convertTo(units).toRelativeDistance()
    }

    private fun getSpeed(lastReset: Instant, distance: Distance): Speed? {
        val duration = Duration.between(lastReset, Instant.now())
        if (duration.isZero || duration.isNegative) return null

        return Speed(distance.distance / duration.seconds, distance.units, TimeUnits.Seconds).convertTo(
            DistanceUnits.Meters, TimeUnits.Seconds
        )
    }

}