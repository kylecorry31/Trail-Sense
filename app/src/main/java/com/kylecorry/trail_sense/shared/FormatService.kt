package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.DistanceUnits
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.compass.CompassDirection
import com.kylecorry.trail_sense.shared.domain.Accuracy
import com.kylecorry.trail_sense.shared.domain.Coordinate

class FormatService(private val context: Context) {

    private val prefs = UserPreferences(context)

    fun formatDegrees(degrees: Float): String {
        return context.getString(R.string.degree_format, degrees)
    }

    fun formatDirection(direction: CompassDirection): String {
        return direction.symbol
    }

    fun formatDistance(distance: Float, units: DistanceUnits): String {
        return when (units) {
            DistanceUnits.Meters -> context.getString(R.string.meters_format, distance)
            DistanceUnits.Kilometers -> context.getString(R.string.kilometers_format, distance)
            DistanceUnits.Feet -> context.getString(R.string.feet_format, distance)
            DistanceUnits.Miles -> context.getString(R.string.miles_format, distance)
        }
    }

    fun formatSmallDistance(distanceMeters: Float): String {
        val base = getBaseUnit()
        return formatDistance(
            LocationMath.convert(distanceMeters, DistanceUnits.Meters, base),
            base
        )
    }

    fun formatLargeDistance(distanceMeters: Float): String {
        return formatSmallDistance(distanceMeters) // TODO
    }

    fun formatAccuracy(accuracy: Accuracy): String {
        return accuracy.toString()
    }

    fun formatSpeed(metersPerSecond: Float): String {
        val distanceUnits = prefs.distanceUnits
        val convertedSpeed = LocationMath.convertToBaseSpeed(metersPerSecond, distanceUnits)
        return if (distanceUnits == UserPreferences.DistanceUnits.Meters) {
            context.getString(R.string.kilometers_per_hour_format, convertedSpeed)
        } else {
            context.getString(R.string.miles_per_hour_format, convertedSpeed)
        }
    }

    fun formatLocation(location: Coordinate): String {
        val formatter = prefs.navigation.locationFormatter
        val lat = formatter.formatLatitude(location)
        val lng = formatter.formatLongitude(location)
        return getFormattedLocation(lat, lng)
    }

    private fun getFormattedLocation(latitude: String, longitude: String): String {
        return context.getString(prefs.navigation.locationFormat, latitude, longitude)
    }

    private fun getBaseUnit(): DistanceUnits {
        val prefUnits = prefs.distanceUnits
        return if (prefUnits == UserPreferences.DistanceUnits.Feet) {
            DistanceUnits.Feet
        } else {
            DistanceUnits.Meters
        }
    }

}