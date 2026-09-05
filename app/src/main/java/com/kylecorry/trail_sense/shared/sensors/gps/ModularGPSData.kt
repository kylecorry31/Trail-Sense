package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.luna.topics.Subscriber
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.time.Instant

class ModularGPSData(
    override var satellites: Int? = null,
    override var satelliteDetails: List<Satellite>? = null,
    override var location: Coordinate = Coordinate.zero,
    override var verticalAccuracy: Float? = null,
    override var horizontalAccuracy: Float? = null,
    override var mslAltitude: Float? = null,
    override var bearing: Bearing? = null,
    override var rawBearing: Float? = null,
    override var bearingAccuracy: Float? = null,
    override var speedAccuracy: Float? = null,
    override var fixTimeElapsedNanos: Long? = null,
    override var quality: Quality = Quality.Unknown,
    override var hasValidReading: Boolean = false,
    override var altitude: Float = 0f,
    override var time: Instant = Instant.now(),
    override var speed: Speed = Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds),
    @Volatile var isTimedOut: Boolean = false
) : ISatelliteGPS {

    // This is a data holder, so it never emits
    override val flow: Flow<Unit> = emptyFlow()

    override fun start(subscriber: Subscriber) {
        // Do nothing
    }

    override fun stop(subscriber: Subscriber?) {
        // Do nothing
    }

    override fun subscribe(subscriber: Subscriber) {
        // Do nothing
    }

    override fun unsubscribe(subscriber: Subscriber) {
        // Do nothing
    }

    override fun unsubscribeAll() {
        // Do nothing
    }

    override suspend fun read(isSatisfied: () -> Boolean) {
        // Do nothing
    }

    fun copyInto(other: ModularGPSData) {
        other.satellites = satellites
        other.satelliteDetails = satelliteDetails
        other.location = location
        other.verticalAccuracy = verticalAccuracy
        other.horizontalAccuracy = horizontalAccuracy
        other.mslAltitude = mslAltitude
        other.bearing = bearing
        other.rawBearing = rawBearing
        other.bearingAccuracy = bearingAccuracy
        other.speedAccuracy = speedAccuracy
        other.fixTimeElapsedNanos = fixTimeElapsedNanos
        other.quality = quality
        other.hasValidReading = hasValidReading
        other.altitude = altitude
        other.time = time
        other.speed = speed
        other.isTimedOut = isTimedOut
    }

    fun populateFromGPS(gps: ISatelliteGPS) {
        satellites = gps.satellites
        satelliteDetails = gps.satelliteDetails
        location = gps.location
        verticalAccuracy = gps.verticalAccuracy
        horizontalAccuracy = gps.horizontalAccuracy
        mslAltitude = gps.mslAltitude
        bearing = gps.bearing
        rawBearing = gps.rawBearing
        bearingAccuracy = gps.bearingAccuracy
        speedAccuracy = gps.speedAccuracy
        fixTimeElapsedNanos = gps.fixTimeElapsedNanos
        quality = gps.quality
        hasValidReading = gps.hasValidReading
        altitude = gps.altitude
        time = gps.time
        speed = gps.speed
    }
}
