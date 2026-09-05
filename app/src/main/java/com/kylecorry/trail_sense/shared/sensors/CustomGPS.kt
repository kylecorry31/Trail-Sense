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
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.gps.BadReadingRejectionGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.CacheGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPipeline
import com.kylecorry.trail_sense.shared.sensors.gps.GPSUpdateResult
import com.kylecorry.trail_sense.shared.sensors.gps.KalmanGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.MeanSeaLevelGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SpeedGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.TimeoutGPSModule
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
        get() {
            // We always want to get the latest location. This is not the best pattern, but it works for now.
            // In the future the consumers should instruct it to pull from latest or this should listen to the cache key and update when it changes.
            pipeline.reinitialize()
            return data.location
        }

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
    private val userPrefs by lazy { UserPreferences(context) }

    private val pipeline = GPSPipeline(
        listOfNotNull(
            BadReadingRejectionGPSModule(),
            MeanSeaLevelGPSModule(),
            SpeedGPSModule(),
            TimeoutGPSModule(this::notifyListeners),
            if (userPrefs.useFilteredGPS) KalmanGPSModule() else null,
            CacheGPSModule()
        )
    )
    private val data: ModularGPSData
        get() = pipeline.reading

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

        // If this is being restarted, reload the value from cache if there's a newer reading there
        if (pipeline.hadValidReading && pipeline.reinitialize()) {
            notifyListeners()
        }

        baseGPS.start(this::onLocationUpdate)
        pipeline.start()
    }

    override fun stopImpl() {
        baseGPS.stop(this::onLocationUpdate)
        pipeline.stop()
    }

    private fun onLocationUpdate(): Boolean {
        val result = updateGPSData()
        if (result == GPSUpdateResult.NewFixAccepted) {
            notifyListeners()
        }
        return true
    }

    private fun updateGPSData(): GPSUpdateResult {
        return pipeline.update(baseGPS)
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
