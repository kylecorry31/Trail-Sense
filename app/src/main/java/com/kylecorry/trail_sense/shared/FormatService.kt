package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.units.*
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trailsensecore.domain.units.Pressure
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import java.time.Duration

class FormatService(private val context: Context) {

    private val v2 by lazy { FormatServiceV2(context) }
    private val prefs by lazy { UserPreferences(context) }

    fun formatDegrees(degrees: Float): String {
        return v2.formatDegrees(degrees, 0, true)
    }

    fun formatDirection(direction: CompassDirection): String {
        return v2.formatDirection(direction)
    }

    fun formatDuration(duration: Duration, short: Boolean = false): String {
        return v2.formatDuration(duration, short)
    }

    fun formatDistance(distance: Distance): String {
        return formatDistance(distance.distance, distance.units)
    }

    fun formatDistance(distance: Float, units: DistanceUnits): String {
        val places = if (units == DistanceUnits.Meters || units == DistanceUnits.Feet){
            0
        } else {
            2
        }

        return v2.formatDistance(Distance(distance, units), places, false)
    }

    fun formatSmallDistance(distanceMeters: Float): String {
        val base = prefs.baseDistanceUnits
        return formatDistance(Distance(distanceMeters, DistanceUnits.Meters).convertTo(base))
    }

    fun formatLargeDistance(distanceMeters: Float): String {
        val distance = Distance.meters(distanceMeters).convertTo(prefs.baseDistanceUnits)
        return formatDistance(distance.toRelativeDistance())
    }

    fun formatQuality(quality: Quality): String {
        return v2.formatQuality(quality)
    }

    fun formatSpeed(metersPerSecond: Float): String {
        return v2.formatSpeed(metersPerSecond)
    }

    fun formatLocation(location: Coordinate, format: CoordinateFormat? = null): String {
        return v2.formatLocation(location, format)
    }

    fun formatPressure(pressure: Float, unit: PressureUnits): String {
        return v2.formatPressure(Pressure(pressure, unit), 1)
    }

    fun formatPercentage(percent: Int): String {
        return v2.formatPercentage(percent.toFloat())
    }

}