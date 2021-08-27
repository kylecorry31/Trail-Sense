package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.Bearing
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.Position

class DestinationPanel(private val view: View) {

    private val beaconName = view.findViewById<TextView>(R.id.beacon_name)
    private val beaconComments = view.findViewById<ImageButton>(R.id.beacon_comment_btn)
    private val beaconDistance = view.findViewById<TextView>(R.id.beacon_distance)
    private val beaconDirection = view.findViewById<TextView>(R.id.beacon_direction)
    private val beaconDirectionCardinal =
        view.findViewById<TextView>(R.id.beacon_direction_cardinal)
    private val beaconElevationView = view.findViewById<LinearLayout>(R.id.beacon_elevation_view)
    private val beaconElevation = view.findViewById<TextView>(R.id.beacon_elevation)
    private val beaconElevationDiff = view.findViewById<TextView>(R.id.beacon_elevation_diff)
    private val beaconEta = view.findViewById<TextView>(R.id.beacon_eta)
    private val navigationService = NavigationService()
    private val formatService = FormatService(view.context)
    private val prefs = UserPreferences(view.context)
    private val nonLinearDistances = prefs.navigation.factorInNonLinearDistance
    private val context = view.context
    private var beacon: Beacon? = null

    init {
        beaconComments.setOnClickListener {
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

        view.visibility = View.VISIBLE

        beacon = destination

        if (!beacon?.comment.isNullOrEmpty()) {
            beaconComments.visibility = View.VISIBLE
        } else {
            beaconComments.visibility = View.GONE
        }

        beaconName.text = destination.name
        updateDestinationDirection(vector.direction)
        updateDestinationElevation(destination.elevation, vector.altitudeChange)
        updateDestinationEta(position, destination)
    }

    fun hide() {
        view.visibility = View.GONE
        beacon = null
    }

    private fun updateDestinationDirection(azimuth: Bearing) {
        beaconDirection.text = formatService.formatDegrees(azimuth.value)
        beaconDirectionCardinal.text = formatService.formatDirection(azimuth.direction)
    }

    private fun updateDestinationEta(position: Position, beacon: Beacon) {
        val d = Distance.meters(position.location.distanceTo(beacon.coordinate))
            .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        beaconDistance.text =
            formatService.formatDistance(d, Units.getDecimalPlaces(d.units), false)
        val eta = navigationService.eta(position, beacon, nonLinearDistances)
        beaconEta.text = context.getString(R.string.eta, formatService.formatDuration(eta, false))
    }

    private fun updateDestinationElevation(destinationElevation: Float?, elevationChange: Float?) {
        if (elevationChange != null && destinationElevation != null) {
            beaconElevationView.visibility = View.VISIBLE

            val destElevationDist =
                Distance.meters(destinationElevation).convertTo(prefs.baseDistanceUnits)
            beaconElevation.text = formatService.formatDistance(
                destElevationDist,
                Units.getDecimalPlaces(destElevationDist.units),
                false
            )

            val direction = when {
                elevationChange == 0.0f -> ""
                elevationChange > 0 -> context.getString(R.string.increase)
                else -> context.getString(R.string.decrease)
            }

            val elevationChangeDist =
                Distance.meters(elevationChange).convertTo(prefs.baseDistanceUnits)

            beaconElevationDiff.text = context.getString(
                R.string.elevation_diff_format,
                direction,
                formatService.formatDistance(
                    elevationChangeDist, Units.getDecimalPlaces(elevationChangeDist.units),
                    false
                )
            )
            val changeColor = when {
                elevationChange >= 0 -> {
                    R.color.positive
                }
                else -> {
                    R.color.negative
                }
            }
            beaconElevationDiff.setTextColor(Resources.color(context, changeColor))
        } else {
            beaconElevationView.visibility = View.GONE
        }
    }

}