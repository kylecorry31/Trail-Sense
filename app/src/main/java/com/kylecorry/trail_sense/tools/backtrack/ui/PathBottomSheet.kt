package com.kylecorry.trail_sense.tools.backtrack.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPathBottomSheetBinding
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.shared.DistanceUtils.isLarge
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.rangeOrNull
import com.kylecorry.trail_sense.tools.backtrack.domain.*
import com.kylecorry.trailsensecore.domain.navigation.NavigationService
import java.time.Duration
import java.time.Instant

class PathBottomSheet : BoundBottomSheetDialogFragment<FragmentPathBottomSheetBinding>() {

    private val navigationService = NavigationService()
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val cache by lazy { Preferences(requireContext()) }
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val throttle = Throttle(20)

    var drawPathToGPS: Boolean = false
        set(value) {
            field = value
            onPathChanged()
        }

    var path: List<WaypointEntity> = emptyList()
        set(value) {
            field = value
            onPathChanged()
        }

    var location: Coordinate? = null
        set(value) {
            field = value
            onPathChanged()
        }

    var azimuth: Float = 0f
        set(value) {
            field = value
            onPathChanged()
        }

    private var pointColoringStyle = PointColoringStyle.None

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pathPointStyle.setOnClickListener {
            Pickers.item(
                requireContext(), "", listOf(
                    getString(R.string.path),
                    getString(R.string.cell_signal),
                    getString(R.string.altitude),
                    getString(R.string.time)
                ),
                defaultSelectedIndex = pointColoringStyle.ordinal
            ) {
                if (it != null) {
                    pointColoringStyle =
                        PointColoringStyle.values().find { style -> style.ordinal == it }
                            ?: PointColoringStyle.None
                    cache.putInt("pref_path_waypoint_style", pointColoringStyle.ordinal)
                    updatePointStyleLegend()
                    onPathChanged()
                }
            }
        }

        val lastStyle = cache.getInt("pref_path_waypoint_style")
        pointColoringStyle =
            PointColoringStyle.values().find { style -> style.ordinal == lastStyle }
                ?: PointColoringStyle.None

        updatePointStyleLegend()
        onPathChanged()
    }

    private fun onPathChanged() {
        if (!isBound || throttle.isThrottled()) {
            return
        }

        val distance = navigationService.getPathDistance(path.map { it.coordinate })
            .convertTo(prefs.baseDistanceUnits).toRelativeDistance()

        val start = path.firstOrNull()?.createdInstant
        val end = path.lastOrNull()?.createdInstant

        binding.pathTimes.text = if (start != null && end != null) {
            formatService.formatTimeSpan(start.toZonedDateTime(), end.toZonedDateTime(), true)
        } else {
            ""
        }

        binding.pathDuration.text = if (start != null && end != null) {
            formatService.formatDuration(Duration.between(start, end), false)
        } else {
            ""
        }

        binding.pathWaypoints.text = path.size.toString()

        binding.pathDistance.text =
            formatService.formatDistance(distance, if (distance.units.isLarge()) 2 else 0, false)

        binding.pathImage.path = if (drawPathToGPS && location != null) {
            path + getGPSWaypoint(path.firstOrNull()?.pathId ?: 0L)
        } else {
            path
        }
        binding.pathImage.location = location
        binding.pathImage.azimuth = azimuth

        binding.pathImage.pointColoringStrategy = when (pointColoringStyle) {
            PointColoringStyle.None -> DefaultPointColoringStrategy(Color.TRANSPARENT)
            PointColoringStyle.CellSignal -> CellSignalPointColoringStrategy()
            PointColoringStyle.Altitude -> {
                val altitudeRange = path.mapNotNull { it.altitude }.rangeOrNull() ?: Range(0f, 0f)
                AltitudePointColoringStrategy(
                    altitudeRange,
                    AppColor.Red.color,
                    AppColor.DarkBlue.color
                )
            }
            PointColoringStyle.Time -> {
                val timeRange = path.map { it.createdInstant }.rangeOrNull() ?: Range(Instant.now(), Instant.now())
                TimePointColoringStrategy(
                    timeRange,
                    Color.WHITE,
                    AppColor.DarkBlue.color
                )
            }
        }

        binding.pathImage.arePointsHighlighted = pointColoringStyle != PointColoringStyle.None
    }

    private fun updatePointStyleLegend() {
        binding.pathPointStyle.text = listOf(
            getString(R.string.path),
            getString(R.string.cell_signal),
            getString(R.string.altitude),
            getString(R.string.time)
        )[pointColoringStyle.ordinal]
    }

    private fun getGPSWaypoint(pathId: Long): WaypointEntity {
        return WaypointEntity(
            location!!.latitude,
            location!!.longitude,
            null,
            Instant.now().toEpochMilli(),
            pathId = pathId,
            cellQualityId = null,
            cellTypeId = null
        )
    }

    private enum class PointColoringStyle {
        None,
        CellSignal,
        Altitude,
        Time
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPathBottomSheetBinding {
        return FragmentPathBottomSheetBinding.inflate(layoutInflater, container, false)
    }
}