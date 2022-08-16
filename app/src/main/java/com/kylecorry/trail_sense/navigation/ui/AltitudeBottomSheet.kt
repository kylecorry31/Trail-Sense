package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentAltitudeHistoryBinding
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import com.kylecorry.trail_sense.weather.infrastructure.persistence.WeatherRepo
import java.time.Duration
import java.time.Instant

class AltitudeBottomSheet : BoundBottomSheetDialogFragment<FragmentAltitudeHistoryBinding>() {

    private val pathService by lazy { PathService.getInstance(requireContext()) }
    private val weatherRepo by lazy { WeatherRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private var units = DistanceUnits.Meters
    private lateinit var chart: SimpleLineChart
    private var backtrackReadings = listOf<Reading<Float>>()
    private var weatherReadings = listOf<Reading<Float>>()

    var backtrackPoints: List<PathPoint>? = null
    var currentAltitude: Reading<Float>? = null

    private val maxHistoryDuration = Duration.ofDays(1)
    private val maxFilterHistoryDuration = maxHistoryDuration.plusHours(6)
    private var historyDuration = maxHistoryDuration

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
            drawGridLines = false,
            minimum = (Instant.now().toEpochMilli() - historyDuration.toMillis()).toFloat(),
            maximum = Instant.now().toEpochMilli().toFloat()
        )
        val path = backtrackPoints
        if (path != null) {
            backtrackReadings = path.mapNotNull { point ->
                point.elevation ?: return@mapNotNull null
                point.time ?: return@mapNotNull null
                Reading(point.elevation, point.time)
            }
            updateChart()
        } else {
            getBacktrackReadings().observe(viewLifecycleOwner) {
                backtrackReadings = it
                updateChart()
            }
        }
        getWeatherReadings().observe(viewLifecycleOwner) {
            weatherReadings = it
            updateChart()
        }

        binding.altitudeHistoryLength.setOnClickListener {
            CustomUiUtils.pickDuration(
                requireContext(),
                historyDuration,
                getString(R.string.elevation_history_length)
            ) {
                if (it != null) {
                    historyDuration = it
                    chart.configureXAxis(
                        labelCount = 0,
                        drawGridLines = false,
                        minimum = (Instant.now()
                            .toEpochMilli() - historyDuration.toMillis()).toFloat(),
                        maximum = Instant.now().toEpochMilli().toFloat()
                    )
                    updateChart()
                }
            }
        }
    }

    private fun updateChart(readings: List<Reading<Float>>) {
        if (!isBound) return

        val data = readings.map {
            it.time.toEpochMilli().toFloat() to Distance.meters(it.value).convertTo(units).distance
        }

        val granularity = Distance.meters(5f).convertTo(units).distance
        val minRange = Distance.meters(30f).convertTo(units).distance
        val range = SimpleLineChart.getYRange(data, granularity, minRange)
        chart.configureYAxis(
            minimum = range.start,
            maximum = range.end,
            granularity = granularity,
            labelCount = 5,
            drawGridLines = true
        )

        chart.plot(
            data, Resources.getAndroidColorAttr(requireContext(), R.attr.colorPrimary),
            filled = true
        )

        binding.altitudeHistoryLength.text =
            getString(R.string.last_duration, formatService.formatDuration(historyDuration))
    }

    private fun updateChart() {
        lifecycleScope.launchWhenResumed {
            val filteredReadings = onDefault {
                val readings =
                    (backtrackReadings + weatherReadings + listOfNotNull(currentAltitude)).sortedBy { it.time }
                        .filter { Duration.between(it.time, Instant.now()) < maxFilterHistoryDuration }

                val start = readings.firstOrNull()?.time ?: Instant.now()

                DataUtils.smooth(
                    readings,
                    0.3f,
                    { _, reading ->
                        Vector2(
                            Duration.between(start, reading.time).toMillis() / 1000f,
                            reading.value
                        )
                    }
                ) { reading, smoothed ->
                    reading.copy(value = smoothed.y)
                }.filter {
                    Duration.between(it.time, Instant.now()).abs() <= historyDuration
                }
            }

            onMain { updateChart(filteredReadings) }
        }
    }

    private fun getWeatherReadings(): LiveData<List<Reading<Float>>> {
        return Transformations.map(weatherRepo.getAllLive()) {
            it.mapNotNull { reading ->
                if (reading.value.altitude == 0f) {
                    return@mapNotNull null
                }
                Reading(reading.value.altitude, reading.time)
            }
        }
    }

    private fun getBacktrackReadings(): LiveData<List<Reading<Float>>> {
        return pathService.getRecentAltitudes(
            Instant.now().minus(maxFilterHistoryDuration)
        )
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAltitudeHistoryBinding {
        return FragmentAltitudeHistoryBinding.inflate(layoutInflater, container, false)
    }

}