package com.kylecorry.trail_sense.tools.speedometer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolSpeedometerBinding
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.infrastructure.sensors.asLiveData
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import java.time.LocalDate

class FragmentToolSpeedometer : BoundFragment<FragmentToolSpeedometerBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val odometer by lazy { sensorService.getOdometer() }
    private val instantSpeedometer by lazy { sensorService.getSpeedometer(true) }
    private val averageSpeedometer by lazy { sensorService.getSpeedometer(false) }
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private val throttle = Throttle(20)

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolSpeedometerBinding {
        return FragmentToolSpeedometerBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.odometerReset.setOnClickListener {
            odometer.reset()
        }
        binding.odometerHolder.visibility = if (prefs.experimentalEnabled) View.VISIBLE else View.GONE
        averageSpeedometer.asLiveData().observe(viewLifecycleOwner, { update() })
        instantSpeedometer.asLiveData().observe(viewLifecycleOwner, { update() })
        odometer.asLiveData().observe(viewLifecycleOwner, { update() })
    }

    private fun update() {
        if (throttle.isThrottled() || context == null) {
            return
        }

        // Real time speed
        binding.instantaneousSpeed.text = formatService.formatSpeed(instantSpeedometer.speed.speed)

        // Average
        binding.averageSpeed.text = if (prefs.backtrackEnabled) {
            getString(
                R.string.value_average,
                formatService.formatSpeed(averageSpeedometer.speed.speed)
            )
        } else {
            null
        }


        // Odometer
        val odometerDistance =
            odometer.distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        val lastReset = odometer.lastReset.toZonedDateTime()
        val dateString = if (lastReset.toLocalDate() == LocalDate.now()) {
            formatService.formatTime(lastReset.toLocalTime(), false)
        } else {
            formatService.formatRelativeDate(lastReset.toLocalDate())
        }
        binding.odometer.text = getString(
            R.string.value_since_time,
            formatService.formatDistance(odometerDistance, if (IsLargeUnitSpecification().isSatisfiedBy(odometerDistance.units)) 2 else 0),
            dateString
        )
    }

}