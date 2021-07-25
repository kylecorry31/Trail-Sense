package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentAltitudeHistoryBinding
import com.kylecorry.trail_sense.shared.BoundBottomSheetDialogFragment
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trail_sense.weather.domain.AltitudeReading
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.infrastructure.sensors.asLiveData
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import java.time.Duration
import java.time.Instant

class AltitudeBottomSheet : BoundBottomSheetDialogFragment<FragmentAltitudeHistoryBinding>() {

    private val backtrackRepo by lazy { WaypointRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private var units = DistanceUnits.Meters
    private val altimeter by lazy { SensorService(requireContext()).getAltimeter(false) }
    private lateinit var chart: SimpleLineChart
    private var readings = listOf<AltitudeReading>()
    private val readingsSinceOpened = mutableListOf<AltitudeReading>()

    // TODO: Allow user configuration of this (maybe directly from the sheet)
    private var maxHistoryDuration = Duration.ofDays(1)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        units = prefs.baseDistanceUnits
        chart = SimpleLineChart(binding.chart, getString(R.string.no_altitude_history))
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true
        )

        chart.configureXAxis(
            labelCount = 0,
            drawGridLines = false
        )
        altimeter.asLiveData().observe(viewLifecycleOwner) {
            readingsSinceOpened.add(AltitudeReading(Instant.now(), altimeter.altitude))
            updateChart(readings + readingsSinceOpened)
        }
        getReadings().observe(viewLifecycleOwner) {
            readings = it.sortedBy { it.time }
                .filter { Duration.between(it.time, Instant.now()).abs() <= maxHistoryDuration }
            updateChart(readings + readingsSinceOpened)
        }
    }

    private fun updateChart(readings: List<AltitudeReading>) {
        val data = readings.map {
            it.time.toEpochMilli().toFloat() to Distance.meters(it.value).convertTo(units).distance
        }
        chart.plot(data, UiUtils.color(requireContext(), R.color.colorPrimary), filled = true)


        // Set title
        if (readings.size >= 2) {
            val totalTime = Duration.between(
                readings.first().time, readings.last().time
            )
            var hours = totalTime.toHours()
            val minutes = totalTime.toMinutes() % 60

            when (hours) {
                0L -> binding.altitudeHistoryLength.text =
                    context?.resources?.getQuantityString(
                        R.plurals.last_minutes,
                        minutes.toInt(),
                        minutes
                    )
                else -> {
                    if (minutes >= 30) hours++
                    binding.altitudeHistoryLength.text =
                        context?.resources?.getQuantityString(
                            R.plurals.last_hours,
                            hours.toInt(),
                            hours
                        )
                }
            }

        }
    }

    private fun getReadings(): LiveData<List<AltitudeReading>> {
        // TODO: Merge with weather readings
        return Transformations.map(backtrackRepo.getWaypoints()) {
            it.mapNotNull { waypoint ->
                waypoint.altitude ?: return@mapNotNull null
                AltitudeReading(waypoint.createdInstant, waypoint.altitude)
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAltitudeHistoryBinding {
        return FragmentAltitudeHistoryBinding.inflate(layoutInflater, container, false)
    }

}