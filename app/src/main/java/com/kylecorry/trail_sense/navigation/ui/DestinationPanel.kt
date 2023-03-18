package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewBeaconDestinationBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import java.time.ZonedDateTime

class DestinationPanel(private val view: View) {

    private val binding = ViewBeaconDestinationBinding.bind(view)
    private val navigationService = NavigationService()
    private val formatService = FormatService.getInstance(view.context)
    private val prefs = UserPreferences(view.context)
    private val context = view.context
    private var beacon: Beacon? = null

    private val commentColor = Resources.androidTextColorSecondary(context)

    init {
        binding.root.setOnClickListener {
            if (!beacon?.comment.isNullOrEmpty()) {
                Alerts.dialog(context, beacon?.name ?: "", beacon?.comment, cancelText = null)
            }
        }
    }

    // TODO: Make this take the calculated values (as a value object)
    fun show(
        position: Position,
        destination: Beacon,
        declination: Float,
        usingTrueNorth: Boolean = true
    ) {
        val vector = navigationService.navigate(position, destination, declination, usingTrueNorth)

        binding.root.isVisible = true

        val destinationChanged = beacon?.id != destination.id
        beacon = destination

        binding.beaconName.text = destination.name
        if (destinationChanged) {
            binding.beaconName.setCompoundDrawables(
                size = Resources.dp(context, 18f).toInt(),
                right = if (!destination.comment.isNullOrEmpty()) R.drawable.ic_tool_notes else null
            )
            CustomUiUtils.setImageColor(binding.beaconName, commentColor)
        }
        updateDestinationDirection(vector.direction)
        updateDestinationElevation(destination.elevation, vector.altitudeChange)
        updateDestinationEta(position, destination)
    }

    fun hide() {
        view.visibility = View.GONE
        beacon = null
    }

    private fun updateDestinationDirection(azimuth: Bearing) {
        binding.beaconDistance.description = formatService.formatDegrees(
            azimuth.value,
            replace360 = true
        ) + " " + formatService.formatDirection(azimuth.direction)
    }

    private fun updateDestinationEta(position: Position, beacon: Beacon) {
        val d = Distance.meters(position.location.distanceTo(beacon.coordinate))
            .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        binding.beaconDistance.title =
            formatService.formatDistance(d, Units.getDecimalPlaces(d.units), false)

        // ETA
        val eta = navigationService.eta(position, beacon)
        binding.beaconEta.title = formatService.formatDuration(eta, false)
        binding.beaconEta.description = formatService.formatTime(
            ZonedDateTime.now().plus(eta).toLocalTime(),
            includeSeconds = false
        )
    }

    private fun updateDestinationElevation(destinationElevation: Float?, elevationChange: Float?) {
        val hasElevation = elevationChange != null && destinationElevation != null
        binding.beaconElevation.isVisible = hasElevation
        if (hasElevation) {
            val elevationChange = elevationChange!!
            val destinationElevation = destinationElevation!!
            val destElevationDist =
                Distance.meters(destinationElevation).convertTo(prefs.baseDistanceUnits)
            binding.beaconElevation.title = formatService.formatDistance(
                destElevationDist,
                Units.getDecimalPlaces(destElevationDist.units),
                false
            )

            val direction = when {
                elevationChange > 0 -> context.getString(R.string.increase)
                else -> ""
            }

            val elevationChangeDist =
                Distance.meters(elevationChange).convertTo(prefs.baseDistanceUnits)

            binding.beaconElevation.description = context.getString(
                R.string.elevation_diff_format,
                direction,
                formatService.formatDistance(
                    elevationChangeDist, Units.getDecimalPlaces(elevationChangeDist.units),
                    false
                )
            )
        }
    }
}