package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.DistanceUnits
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.compass.CompassDirection
import com.kylecorry.trail_sense.shared.domain.Accuracy
import com.kylecorry.trail_sense.shared.domain.Coordinate
import java.time.Duration
import java.time.LocalDateTime

class FormatService(private val context: Context) {

    private val prefs = UserPreferences(context)

    fun formatDegrees(degrees: Float): String {
        return context.getString(R.string.degree_format, degrees)
    }

    fun formatDirection(direction: CompassDirection): String {
        return direction.symbol
    }

    fun formatDuration(duration: Duration, short: Boolean = false): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60

        return if (short){
            when (hours) {
                0L -> context.getString(R.string.duration_minute_format, minutes)
                else -> context.getString(R.string.duration_hour_format, hours)
            }
        } else {
            when {
                hours == 0L -> context.getString(R.string.duration_minute_format, minutes)
                minutes == 0L -> context.getString(R.string.duration_hour_format, hours)
                else -> context.getString(R.string.duration_hour_minute_format, hours, minutes)
            }
        }
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
        val units = getLargeDistanceUnits(distanceMeters)
        return formatDistance(LocationMath.convert(distanceMeters, DistanceUnits.Meters, units), units)
    }

    fun formatAccuracy(accuracy: Accuracy): String {
        return when(accuracy){
            Accuracy.Low -> context.getString(R.string.accuracy_low)
            Accuracy.Medium -> context.getString(R.string.accuracy_medium)
            Accuracy.High -> context.getString(R.string.accuracy_high)
            else -> context.getString(R.string.accuracy_unknown)
        }
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

    private fun getLargeDistanceUnits(meters: Float): DistanceUnits {
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