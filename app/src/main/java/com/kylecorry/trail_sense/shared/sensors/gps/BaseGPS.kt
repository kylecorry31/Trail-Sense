package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import java.time.Instant


abstract class BaseGPS(
    protected val baseGPS: ISatelliteGPS
) : AbstractSensor(), ISatelliteGPS {

    override val hasValidReading: Boolean
        get() = _hasValidReading
    override val satellites: Int?
        get() = _satellites
    override val quality: Quality
        get() = _quality
    override val rawBearing: Float?
        get() = _rawBearing
    override val satelliteDetails: List<Satellite>?
        get() = _satelliteDetails
    override val horizontalAccuracy: Float?
        get() = _horizontalAccuracy
    override val verticalAccuracy: Float?
        get() = _verticalAccuracy
    override val location: Coordinate
        get() = _location
    override val speed: Speed
        get() = _speed
    override val speedAccuracy: Float?
        get() = _speedAccuracy
    override val time: Instant
        get() = _time
    override val altitude: Float
        get() = _altitude
    override val bearing: Bearing?
        get() = _bearing
    override val bearingAccuracy: Float?
        get() = _bearingAccuracy
    override val fixTimeElapsedNanos: Long?
        get() = _fixTimeElapsedNanos
    override val mslAltitude: Float?
        get() = _mslAltitude

    protected var _altitude = 0f
    protected var _time = Instant.now()
    protected var _quality = Quality.Unknown
    protected var _horizontalAccuracy: Float? = null
    protected var _verticalAccuracy: Float? = null
    protected var _satellites: Int? = null
    protected var _speed: Speed = Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds)
    protected var _location = Coordinate.zero
    protected var _mslAltitude: Float? = null
    protected var _satelliteDetails: List<Satellite>? = null
    protected var _hasValidReading = false
    protected var _fixTimeElapsedNanos: Long? = null
    protected var _rawBearing: Float? = null
    protected var _bearing: Bearing? = null
    protected var _bearingAccuracy: Float? = null
    protected var _speedAccuracy: Float? = null

    @Volatile
    private var isStarted = false

    init {
        if (baseGPS.hasValidReading) {
            updateFromBase()
        }
    }

    protected open fun updateFromBase() {
        _location = baseGPS.location
        _speed = baseGPS.speed
        _verticalAccuracy = baseGPS.verticalAccuracy
        _time = baseGPS.time
        _fixTimeElapsedNanos = baseGPS.fixTimeElapsedNanos
        _horizontalAccuracy = baseGPS.horizontalAccuracy
        _quality = baseGPS.quality
        _satellites = baseGPS.satellites
        _satelliteDetails = baseGPS.satelliteDetails
        _mslAltitude = baseGPS.mslAltitude
        _altitude = baseGPS.altitude
        _rawBearing = baseGPS.rawBearing
        _bearing = baseGPS.bearing
        _bearingAccuracy = baseGPS.bearingAccuracy
        _speedAccuracy = baseGPS.speedAccuracy
        _speed = baseGPS.speed
    }


    override fun startImpl() {
        isStarted = true
        baseGPS.start(this::onLocationUpdateInternal)
    }

    override fun stopImpl() {
        isStarted = false
        baseGPS.stop(this::onLocationUpdateInternal)
    }

    private fun onLocationUpdateInternal(): Boolean {
        onLocationUpdate()
        return true
    }

    protected open fun onLocationUpdate() {
        updateFromBase()
    }
}
