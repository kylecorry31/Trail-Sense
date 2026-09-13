package com.kylecorry.trail_sense.shared.sensors.altimeter

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.logging.Logger
import java.time.Instant

class DigitalElevationModel(private val gps: IGPS) : AbstractSensor(),
    IGPS {

    private val updateTask = BackgroundTask {
        queue.enqueue {
            try {
                val location = gps.location
                val gpsIsValid = gps.hasValidReading
                val fixTimeElapsedNanos = gps.eventTimeElapsedNanos
                val fixTime = gps.eventTime
                demAltitude = DEM.getElevation(location).elevation
                demEventTimeElapsedNanos = fixTimeElapsedNanos
                demEventTime = fixTime
                onMain {
                    if (gpsIsValid) {
                        notifyListeners()
                    }
                }
            } catch (e: Exception) {
                getAppService<Logger>().error("DigitalElevationModel", "Unable to get DEM elevation", e)
            }
        }
    }
    private val queue = CoroutineQueueRunner(2)
    private var demAltitude: Float? = null
    private var demEventTimeElapsedNanos: Long? = null
    private var demEventTime: Instant? = null

    private fun onUpdate(): Boolean {
        updateTask.start()
        return true
    }

    override fun startImpl() {
        onUpdate()
        gps.start(this::onUpdate)
    }

    override fun stopImpl() {
        gps.stop(this::onUpdate)
        queue.cancel()
        updateTask.stop()
    }

    override val hasValidReading: Boolean
        get() = demAltitude != null

    override val altitude: Float
        get() = demAltitude ?: 0f
    override val mslAltitude: Float?
        get() = altitude

    override val location: Coordinate
        get() = gps.location
    override val verticalAccuracy: Float?
        get() = 30f
    override val horizontalAccuracy: Float?
        get() = gps.horizontalAccuracy
    override val bearing: Bearing?
        get() = gps.bearing
    override val rawBearing: Float?
        get() = gps.rawBearing
    override val bearingAccuracy: Float?
        get() = gps.bearingAccuracy
    override val speedAccuracy: Float?
        get() = gps.speedAccuracy
    override var eventTimeElapsedNanos: Long
        get() = demEventTimeElapsedNanos ?: gps.eventTimeElapsedNanos
        set(_) {}
    override var eventTime: Instant
        get() = demEventTime ?: gps.eventTime
        set(_) {}
    override val speed: Speed
        get() = gps.speed
}
