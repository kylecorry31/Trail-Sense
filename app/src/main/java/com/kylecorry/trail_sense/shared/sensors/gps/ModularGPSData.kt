package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.luna.topics.Subscriber
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class ModularGPSData(
    override var satellites: Int?,
    override var satelliteDetails: List<Satellite>?,
    override var location: Coordinate,
    override var verticalAccuracy: Float?,
    override var horizontalAccuracy: Float?,
    override var mslAltitude: Float?,
    override var bearing: Bearing?,
    override var rawBearing: Float?,
    override var bearingAccuracy: Float?,
    override var speedAccuracy: Float?,
    override var fixTimeElapsedNanos: Long?,
    override var quality: Quality,
    override var hasValidReading: Boolean,
    override var flow: Flow<Unit>,
    override var altitude: Float,
    override var time: Instant,
    override var speed: Speed
) : ISatelliteGPS {
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
