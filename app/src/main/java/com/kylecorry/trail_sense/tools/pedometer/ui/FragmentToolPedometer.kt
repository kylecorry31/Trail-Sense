package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolPedometerBinding
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.odometer.Odometer
import java.time.LocalDate

class FragmentToolPedometer : BoundFragment<FragmentToolPedometerBinding>() {

    private val odometer by lazy { Odometer(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private val throttle = Throttle(20)

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolPedometerBinding {
        return FragmentToolPedometerBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.resetBtn.setOnClickListener {
            // TODO: Clear entries from DB
            odometer.reset()
        }
        // TODO: Get distance from DB as live data instead
        odometer.asLiveData().observe(viewLifecycleOwner, { update() })
    }

    private fun update() {
        if (throttle.isThrottled() || context == null) {
            return
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

        binding.pedometerTitle.title.text = formatService.formatDistance(
            odometerDistance,
            Units.getDecimalPlaces(odometerDistance.units),
            false
        )

        binding.pedometerTitle.subtitle.text = getString(R.string.since_time, dateString)
    }

}