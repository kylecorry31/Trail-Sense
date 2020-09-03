package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.*
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.system.UiUtils

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
            if (beacon?.comment != null) {
                UiUtils.alert(context, beacon?.name ?: "", beacon?.comment ?: "")
            }
        }
    }

    fun show(
        position: Position,
        destination: Beacon,
        declination: Float,
        usingTrueNorth: Boolean = true
    ) {
        val vector = navigationService.navigate(position, destination, declination, usingTrueNorth)

        view.visibility = View.VISIBLE

        beacon = destination

        if (destination.comment != null) {
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
        beaconDistance.text =
            formatService.formatLargeDistance(position.location.distanceTo(beacon.coordinate))
        val eta = navigationService.eta(position, beacon, nonLinearDistances)
        beaconEta.text = context.getString(R.string.eta, formatService.formatDuration(eta, false))
    }

    private fun updateDestinationElevation(destinationElevation: Float?, elevationChange: Float?) {
        if (elevationChange != null && destinationElevation != null) {
            beaconElevationView.visibility = View.VISIBLE

            beaconElevation.text = formatService.formatSmallDistance(destinationElevation)

            val direction = when {
                elevationChange == 0.0f -> ""
                elevationChange > 0 -> context.getString(R.string.increase)
                else -> context.getString(R.string.decrease)
            }

            beaconElevationDiff.text = context.getString(
                R.string.elevation_diff_format,
                direction,
                formatService.formatSmallDistance(elevationChange)
            )
            val changeColor = when {
                elevationChange >= 0 -> {
                    R.color.positive
                }
                else -> {
                    R.color.negative
                }
            }
            beaconElevationDiff.setTextColor(UiUtils.color(context, changeColor))
        } else {
            beaconElevationView.visibility = View.GONE
        }
    }

}