package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
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
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.shared.debugging.DebugElevationsCommand
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.ceres.chart.Chart
import com.kylecorry.ceres.chart.data.AreaChartLayer
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import com.kylecorry.trail_sense.weather.infrastructure.persistence.WeatherRepo
import java.time.Duration
import java.time.Instant

class AltitudeBottomSheet : BoundBottomSheetDialogFragment<FragmentAltitudeHistoryBinding>() {

    private val pathService by lazy { PathService.getInstance(requireContext()) }
    private val weatherRepo by lazy { WeatherRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private var units = DistanceUnits.Meters
    private var backtrackReadings = listOf<Reading<Float>>()
    private var weatherReadings = listOf<Reading<Float>>()
    private var startTime = Instant.now()

    var backtrackPoints: List<PathPoint>? = null
    var currentAltitude: Reading<Float>? = null

    private val maxHistoryDuration = Duration.ofDays(1)
    private val maxFilterHistoryDuration = maxHistoryDuration.plusHours(6)
    private var historyDuration = maxHistoryDuration

    private val elevationLine by lazy {
        val color = Resources.getAndroidColorAttr(requireContext(), R.attr.colorPrimary)
        AreaChartLayer(
            emptyList(),
            color,
            color.withAlpha(50)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        units = prefs.baseDistanceUnits
        binding.chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true
        )

        binding.chart.configureXAxis(
            labelCount = 7,
            drawGridLines = false,
            labelFormatter = HourChartLabelFormatter(requireContext()) { startTime }
        )

        binding.chart.emptyText = requireContext().getString(R.string.no_data)

        binding.chart.plot(elevationLine)

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
                    updateChart()
                }
            }
        }
    }

    private fun updateChart(readings: List<Reading<Float>>) {
        if (!isBound) return

        startTime = readings.firstOrNull()?.time

        val data = Chart.getDataFromReadings(readings) {
            Distance.meters(it).convertTo(units).distance
        }

        val margin = Distance.meters(10f).convertTo(units).distance
        val minRange = Distance.meters(100f).convertTo(units).distance
        val range = Chart.getYRange(data, margin, minRange)
        binding.chart.configureYAxis(
            minimum = range.start,
            maximum = range.end,
            labelCount = 5,
            drawGridLines = true
        )


        elevationLine.data = data
        elevationLine.fillTo = range.start

        binding.altitudeHistoryLength.text =
            getString(R.string.last_duration, formatService.formatDuration(historyDuration))
    }

    private fun updateChart() {
        inBackground {
            val filteredReadings = onDefault {
                val readings =
                    (backtrackReadings + weatherReadings + listOfNotNull(currentAltitude)).sortedBy { it.time }
                        .filter {
                            Duration.between(
                                it.time,
                                Instant.now()
                            ) < maxFilterHistoryDuration
                        }


                val smoothed = DataUtils.smoothTemporal(readings, 0.3f)

                onIO {
                    DebugElevationsCommand(requireContext(), readings, smoothed).execute()
                }

                smoothed.filter {
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