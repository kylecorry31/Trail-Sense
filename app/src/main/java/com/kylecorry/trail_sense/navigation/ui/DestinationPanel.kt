package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewBeaconDestinationBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Position
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences

class DestinationPanel(private val view: View) {

    private val binding = ViewBeaconDestinationBinding.bind(view)
    private val navigationService = NavigationService()
    private val formatService = FormatService(view.context)
    private val prefs = UserPreferences(view.context)
    private val context = view.context
    private var beacon: Beacon? = null

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

        beacon = destination

        binding.beaconName.text = destination.name
        updateDestinationDirection(vector.direction)
        updateDestinationElevation(destination.elevation, vector.altitudeChange)
        updateDestinationEta(position, destination)
    }

    fun hide() {
        view.visibility = View.GONE
        beacon = null
    }

    private fun updateDestinationDirection(azimuth: Bearing) {
        binding.beaconDirection.title = formatService.formatDegrees(azimuth.value, replace360 = true)
        binding.beaconDirection.description = formatService.formatDirection(azimuth.direction)
    }

    private fun updateDestinationEta(position: Position, beacon: Beacon) {
        val d = Distance.meters(position.location.distanceTo(beacon.coordinate))
            .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        binding.beaconDistance.title =
            formatService.formatDistance(d, Units.getDecimalPlaces(d.units), false)
        val eta = navigationService.eta(position, beacon)
        binding.beaconDistance.description =
            context.getString(R.string.eta, formatService.formatDuration(eta, false))
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
                elevationChange == 0.0f -> ""
                elevationChange > 0 -> context.getString(R.string.increase)
                else -> context.getString(R.string.dash)
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