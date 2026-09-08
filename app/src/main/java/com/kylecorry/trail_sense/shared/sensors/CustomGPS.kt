package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.luna.subscriptions.generic.Subscription
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPipelineConsumer
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SharedGPSPipeline
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
        get() {
            val accuracy = horizontalAccuracy
            return when {
                accuracy != null && accuracy < 8 -> Quality.Good
                accuracy != null && accuracy < 16 -> Quality.Moderate
                accuracy != null -> Quality.Poor
                else -> Quality.Unknown
            }
        }
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
    private val mainHandler = Handler(Looper.getMainLooper())
    private val consumer = GPSPipelineConsumer(
        SharedGPSPipeline.getInstance(),
        this::notifyListenersOnMain
    )
    private val data: ModularGPSData
        get() = consumer.reading

    private val updates = Subscription<ModularGPSData>(
        replay = 1, // Replay is temporary until the luna onSubscription change is in place to avoid missed readings
        onStart = { withContext(NonCancellable) { if (consumer.start()) notifyListenersOnMain() } },
        onStop = { withContext(NonCancellable) { consumer.stop() } }
    )

    @SuppressLint("MissingPermission")
    override fun startImpl() {
        if (!GPS.isAvailable(context)) {
            return
        }

        updates.subscribe(this::updateGPSData)
        baseGPS.start(this::onLocationUpdate)
    }

    override fun stopImpl() {
        baseGPS.stop(this::onLocationUpdate)
        updates.unsubscribe(this::updateGPSData)
    }

    private fun onLocationUpdate(): Boolean {
        updates.publish(ModularGPSData().also { it.populateFromGPS(baseGPS) })
        return true
    }

    private suspend fun updateGPSData(reading: ModularGPSData) {
        if (consumer.update(reading)) {
            notifyListenersOnMain()
        }
    }

    private fun notifyListenersOnMain() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            notifyListeners()
        } else {
            mainHandler.post { notifyListeners() }
        }
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
