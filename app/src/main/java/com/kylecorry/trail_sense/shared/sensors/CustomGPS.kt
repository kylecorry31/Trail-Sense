package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPipelineConsumer
import com.kylecorry.trail_sense.shared.sensors.gps.SharedGPSPipeline
import java.time.Duration
import java.time.Instant


class CustomGPS(
    private val context: Context,
    private val gpsFrequency: Duration = SensorService.DEFAULT_GPS_FREQUENCY,
) : AbstractSensor(), ISatelliteGPS {

    override val hasValidReading: Boolean
        get() = hadRecentValidReading()

    override val satellites: Int?
        get() = data.satellites

    override val quality: Quality
        get() = data.quality
    override val rawBearing: Float?
        get() = data.rawBearing
    override val satelliteDetails: List<Satellite>?
        get() = data.satelliteDetails

    override val horizontalAccuracy: Float?
        get() = data.horizontalAccuracy

    override val verticalAccuracy: Float?
        get() = data.verticalAccuracy

    override val location: Coordinate
        get() = data.location

    override val speed: Speed
        get() = data.speed
    override val speedAccuracy: Float?
        get() = data.speedAccuracy

    override val time: Instant
        get() = data.time

    override val altitude: Float
        get() = data.altitude
    override val bearing: Bearing?
        get() = data.bearing
    override val bearingAccuracy: Float?
        get() = data.bearingAccuracy

    override val fixTimeElapsedNanos: Long?
        get() = data.fixTimeElapsedNanos

    override val mslAltitude: Float?
        get() = data.mslAltitude

    val isTimedOut: Boolean
        get() = data.isTimedOut

    private val baseGPS: ISatelliteGPS by lazy {
        GPS(context.applicationContext, frequency = gpsFrequency)
    }
    private val consumer = GPSPipelineConsumer(
        SharedGPSPipeline.getInstance(),
        this::notifyListeners
    )
    private val data: ModularGPSData
        get() = consumer.reading

    init {
        if (baseGPS.hasValidReading) {
            updateGPSData()
        }
    }

    @SuppressLint("MissingPermission")
    override fun startImpl() {
        if (!GPS.isAvailable(context)) {
            return
        }

        consumer.start()
        baseGPS.start(this::onLocationUpdate)
    }

    override fun stopImpl() {
        baseGPS.stop(this::onLocationUpdate)
        consumer.stop()
    }

    private fun onLocationUpdate(): Boolean {
        if (updateGPSData()) {
            notifyListeners()
        }
        return true
    }

    private fun updateGPSData(): Boolean {
        return consumer.update(baseGPS)
    }

    private fun hadRecentValidReading(): Boolean {
        val last = time
        val now = Instant.now()
        return Duration.between(last, now) <= RECENT_READING_THRESHOLD &&
                location != Coordinate.zero
    }

    companion object {
        private val RECENT_READING_THRESHOLD: Duration = Duration.ofMinutes(2)
    }
}
