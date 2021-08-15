package com.kylecorry.trail_sense.shared.sensors.odometer

import android.content.Context
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.ApproximateCoordinate
import com.kylecorry.trailsensecore.domain.geo.specifications.LocationChangedSpecification
import com.kylecorry.trailsensecore.domain.geo.specifications.LocationIsAccurateSpecification
import com.kylecorry.trailsensecore.infrastructure.sensors.odometer.IOdometer
import java.time.Instant
import java.time.LocalDate

class Odometer(private val context: Context): AbstractSensor(), IOdometer {

    private val cache by lazy { Preferences(context) }
    private val prefs by lazy { UserPreferences(context) }

    override val distance: Distance
        get() = Distance.meters(cache.getFloat(CACHE_KEY) ?: 0f)

    val lastReset: Instant
        get() = cache.getInstant(LAST_RESET) ?: Instant.now()

    override val hasValidReading: Boolean
        get() = cache.contains(CACHE_KEY)

    private val intervalometer = Timer {
        notifyListeners()
    }

    override fun startImpl() {
        intervalometer.interval(1000)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }

    fun addLocation(location: ApproximateCoordinate){
        if (LocationIsAccurateSpecification().not().isSatisfiedBy(location)){
            // Location is too inaccurate to use
            return
        }

        var distance: Float
        synchronized(this) {
            val loc = cache.getCoordinate(LAST_LOCATION)
            val lastLocation = if (loc != null) Coordinate(loc.latitude, loc.longitude) else null
            val lastAccuracy = cache.getFloat(LAST_ACCURACY) ?: 0f
            if (lastLocation == null) {
                cache.putCoordinate(LAST_LOCATION, location.coordinate)
                cache.putFloat(LAST_ACCURACY, location.accuracy.meters().distance)
                return
            }

            val moved = LocationChangedSpecification(ApproximateCoordinate.from(lastLocation, Distance.meters(lastAccuracy)), prefs.odometerDistanceThreshold)
            if (moved.not().isSatisfiedBy(location)){
                return
            }

            distance = lastLocation.distanceTo(location.coordinate)
            cache.putCoordinate(LAST_LOCATION, location.coordinate)
            cache.putFloat(LAST_ACCURACY, location.accuracy.meters().distance)
        }

        addDistance(Distance.meters(distance))
    }

    fun addDistance(distance: Distance){
        synchronized(this) {
            val lastReset = cache.getInstant(LAST_RESET)
            if (lastReset != null && lastReset.toZonedDateTime().toLocalDate() != LocalDate.now() && prefs.resetOdometerDaily){
                // Reset it daily
                cache.putFloat(CACHE_KEY, 0f)
                cache.remove(LAST_LOCATION)
                cache.putInstant(LAST_RESET, Instant.now())
            } else if (lastReset == null){
                cache.putInstant(LAST_RESET, Instant.now())
            }
            val meters = distance.meters().distance + this.distance.meters().distance
            cache.putFloat(CACHE_KEY, meters)
            notifyListeners()
        }
    }

    fun reset(){
        synchronized(this) {
            cache.putFloat(CACHE_KEY, 0f)
            cache.remove(LAST_LOCATION)
            cache.putInstant(LAST_RESET, Instant.now())
            notifyListeners()
        }
    }

    companion object {
        private const val CACHE_KEY = "odometer_distance"
        private const val LAST_LOCATION = "last_odometer_location"
        private const val LAST_RESET = "last_odometer_reset"
        private const val LAST_ACCURACY = "last_odometer_accuracy"
    }

}