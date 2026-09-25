package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.andromeda.sense.location.GNSSSatelliteStatusSensor
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.GPSPowerUsage
import com.kylecorry.andromeda.sense.location.LocationRequestConfig
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.luna.subscriptions.generic.Subscription
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.sensors.gps.ISatelliteGPS
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPipelineConsumer
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SharedGPSPipeline
import com.kylecorry.trail_sense.shared.sensors.gps.age
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong


class CustomGPS(
    private val context: Context,
    private val gpsFrequency: Duration = SensorService.DEFAULT_GPS_FREQUENCY,
    private val tag: String? = null,
) : AbstractSensor(), ISatelliteGPS {

    private val id = nextId.incrementAndGet()
    private val logger by lazy { getAppService<Logger>() }

    override val hasValidReading: Boolean
        get() = hadRecentValidReading()

    @Volatile
    private var satelliteCount: Int? = null

    override val satellites: Int?
        get() = satelliteCount

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

    @Volatile
    private var currentSatelliteDetails: List<Satellite>? = null

    override val satelliteDetails: List<Satellite>?
        get() = currentSatelliteDetails

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

    override var eventTime: Instant
        get() = data.eventTime
        set(_) {}

    override val altitude: Float
        get() = data.altitude
    override val bearing: Bearing?
        get() = data.bearing
    override val bearingAccuracy: Float?
        get() = data.bearingAccuracy

    override var eventTimeElapsedNanos: Long
        get() = data.eventTimeElapsedNanos
        set(_) {}

    override val mslAltitude: Float? = null

    val isTimedOut: Boolean
        get() = consumer.reading.isTimedOut

    private val baseGPS: GPS by lazy {
        val powerUsage = UserPreferences(context).gps.powerMode.powerUsage ?: GPSPowerUsage.High
        GPS(
            context.applicationContext,
            LocationRequestConfig(frequency = gpsFrequency, powerUsage = powerUsage)
        )
    }
    private val satelliteStatusSensor by lazy {
        GNSSSatelliteStatusSensor(context.applicationContext)
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeProvider = SystemTimeProvider()
    private val consumer = GPSPipelineConsumer(
        SharedGPSPipeline.getInstance(),
        this::notifyListenersOnMain
    )

    private val data: ModularGPSData
        get() = consumer.reading

    private val updates = Subscription<ModularGPSData>(
        onStart = {
            baseGPS.start(this@CustomGPS::onLocationUpdate)
            logger.debug(TAG, "Started GPS $id ($tag, ${gpsFrequency.toMillis()}ms)")
            satelliteStatusSensor.start(this@CustomGPS::onGnssStatusUpdate)
            updateSatelliteStatus()
            val startupReading = ModularGPSData().also { it.populateFromGPS(baseGPS) }
            if (consumer.start(startupReading)) notifyListenersOnMain()
        },
        onStop = {
            baseGPS.stop(this@CustomGPS::onLocationUpdate)
            logger.debug(TAG, "Stopped GPS $id ($tag, ${gpsFrequency.toMillis()}ms)")
            satelliteStatusSensor.stop(this@CustomGPS::onGnssStatusUpdate)
            consumer.stop()
        }
    )

    override fun startImpl() {
        if (!GPS.isAvailable(context)) {
            return
        }

        updates.subscribe(this::updateGPSData)
    }

    override fun stopImpl() {
        updates.unsubscribe(this::updateGPSData)
    }

    private fun onLocationUpdate(): Boolean {
        updates.publish(ModularGPSData().also { it.populateFromGPS(baseGPS) })
        return true
    }

    private fun onGnssStatusUpdate(): Boolean {
        updateSatelliteStatus()
        return true
    }

    private fun updateSatelliteStatus() {
        satelliteCount = satelliteStatusSensor.satellites
        currentSatelliteDetails = satelliteStatusSensor.satelliteDetails
    }

    private suspend fun updateGPSData(reading: ModularGPSData) {
        if (consumer.update(reading)) {
            notifyListenersOnMain()
        }
    }

    private fun notifyListenersOnMain() {
        mainHandler.post { notifyListeners() }
    }

    private fun hadRecentValidReading(): Boolean {
        return age(timeProvider) <= RECENT_READING_THRESHOLD &&
                location != Coordinate.zero
    }

    companion object {
        private const val TAG = "CustomGPS"
        private val nextId = AtomicLong(0)
        private val RECENT_READING_THRESHOLD: Duration = Duration.ofMinutes(2)
    }
}
