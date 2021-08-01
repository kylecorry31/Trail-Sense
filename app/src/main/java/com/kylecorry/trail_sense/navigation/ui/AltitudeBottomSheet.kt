package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentAltitudeHistoryBinding
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trail_sense.weather.domain.AltitudeReading
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import com.kylecorry.trailsensecore.domain.geo.Path
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.BoundBottomSheetDialogFragment
import java.time.Duration
import java.time.Instant

class AltitudeBottomSheet : BoundBottomSheetDialogFragment<FragmentAltitudeHistoryBinding>() {

    private val backtrackRepo by lazy { WaypointRepo.getInstance(requireContext()) }
    private val weatherRepo by lazy { PressureRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private var units = DistanceUnits.Meters
    private lateinit var chart: SimpleLineChart
    private var backtrackReadings = listOf<AltitudeReading>()
    private var weatherReadings = listOf<AltitudeReading>()

    var backtrackPath: Path? = null
    var currentAltitude: AltitudeReading? = null

    // TODO: Allow user configuration of this (maybe directly from the sheet)
    private var maxHistoryDuration = Duration.ofDays(1)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        units = prefs.baseDistanceUnits
        chart = SimpleLineChart(binding.chart, getString(R.string.no_data))
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true
        )

        chart.configureXAxis(
            labelCount = 0,
            drawGridLines = false
        )
        val path = backtrackPath
        if (path != null) {
            backtrackReadings = path.points.filter {
                Duration.between(it.time, Instant.now()).abs() <= maxHistoryDuration
            }.mapNotNull { point ->
                point.elevation ?: return@mapNotNull null
                point.time ?: return@mapNotNull null
                AltitudeReading(point.time!!, point.elevation!!)
            }
            updateChart()
        } else {
            getBacktrackReadings().observe(viewLifecycleOwner) {
                backtrackReadings = it.filter {
                    Duration.between(it.time, Instant.now()).abs() <= maxHistoryDuration
                }
                updateChart()
            }
        }
        getWeatherReadings().observe(viewLifecycleOwner) {
            weatherReadings =
                it.filter { Duration.between(it.time, Instant.now()).abs() <= maxHistoryDuration }
            updateChart()
        }
    }

    private fun updateChart() {
        val readings =
            (backtrackReadings + weatherReadings + listOfNotNull(currentAltitude)).sortedBy { it.time }
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

    private fun getWeatherReadings(): LiveData<List<AltitudeReading>> {
        return Transformations.map(weatherRepo.getPressures()) {
            it.mapNotNull { reading ->
                if (reading.altitude == 0f) {
                    return@mapNotNull null
                }
                AltitudeReading(Instant.ofEpochMilli(reading.time), reading.altitude)
            }
        }
    }

    private fun getBacktrackReadings(): LiveData<List<AltitudeReading>> {
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