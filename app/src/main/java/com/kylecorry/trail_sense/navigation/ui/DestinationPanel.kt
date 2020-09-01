package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.*
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.formatHM
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
    private val context = view.context
    private var beacon: Beacon? = null
    private var prefs = UserPreferences(view.context)

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
        updateDestinationEta(vector.distance, position.speed)
    }

    fun hide() {
        view.visibility = View.GONE
        beacon = null
    }

    private fun updateDestinationDirection(azimuth: Bearing) {
        beaconDirection.text = context.getString(R.string.degree_format, azimuth.value)
        beaconDirectionCardinal.text = azimuth.direction.symbol
    }

    private fun updateDestinationEta(distance: Float, speed: Float) {
        beaconDistance.text =
            formatDistance(
                toUnits(distance, getDistanceUnits(distance)),
                getDistanceUnits(distance)
            )
        val eta = navigationService.eta(distance, speed)?.formatHM(true)
        beaconEta.text =
            if (eta == null) context.getString(R.string.distance_away) else context.getString(
                R.string.eta,
                eta
            )
    }

    private fun updateDestinationElevation(destinationElevation: Float?, elevationChange: Float?) {
        if (elevationChange != null && destinationElevation != null) {
            beaconElevationView.visibility = View.VISIBLE
            // TODO: Get actual distance units

            val desiredUnits = if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) {
                DistanceUnits.Meters
            } else {
                DistanceUnits.Feet
            }

            beaconElevation.text =
                formatDistance(toUnits(destinationElevation, desiredUnits), desiredUnits)

            val direction = when {
                elevationChange == 0.0f -> ""
                elevationChange > 0 -> context.getString(R.string.increase)
                else -> context.getString(R.string.decrease)
            }

            beaconElevationDiff.text = context.getString(
                R.string.elevation_diff_format,
                direction,
                formatDistance(toUnits(elevationChange, desiredUnits), desiredUnits)
            )
            val changeColor = when {
                elevationChange >= 0 -> {
                    R.color.positive
                }
                else -> {
                    R.color.negative
                }
            }
            beaconElevationDiff.setTextColor(context.resources.getColor(changeColor, null))
        } else {
            beaconElevationView.visibility = View.GONE
        }
    }

    private fun toUnits(meters: Float, units: DistanceUnits): Float {
        return LocationMath.convert(meters, DistanceUnits.Meters, units)
    }

    private fun formatDistance(distance: Float, units: DistanceUnits): String {
        return when (units) {
            DistanceUnits.Meters -> context.getString(R.string.meters_format, distance)
            DistanceUnits.Kilometers -> context.getString(R.string.kilometers_format, distance)
            DistanceUnits.Feet -> context.getString(R.string.feet_format, distance)
            DistanceUnits.Miles -> context.getString(R.string.miles_format, distance)
        }
    }

    private fun getDistanceUnits(meters: Float): DistanceUnits {
        val units = prefs.distanceUnits

        if (units == UserPreferences.DistanceUnits.Feet) {
            val feetThreshold = 1000
            val feet = LocationMath.convert(meters, DistanceUnits.Meters, DistanceUnits.Feet)
            return if (feet >= feetThreshold) {
                DistanceUnits.Miles
            } else {
                DistanceUnits.Feet
            }
        } else {
            val meterThreshold = 999
            return if (meters >= meterThreshold) {
                DistanceUnits.Kilometers
            } else {
                DistanceUnits.Meters
            }
        }
    }

}