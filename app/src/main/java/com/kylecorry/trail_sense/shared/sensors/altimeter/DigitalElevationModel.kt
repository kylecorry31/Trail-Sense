package com.kylecorry.trail_sense.shared.sensors.altimeter

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.dem.DEM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant

class DigitalElevationModel(private val context: Context, private val gps: IGPS) : AbstractSensor(),
    IGPS {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val queue = CoroutineQueueRunner(2)
    private var demAltitude: Float? = null
    private var job: Job? = null

    private fun onUpdate(): Boolean {
        job = scope.launch {
            queue.enqueue {
                try {
                    val location = gps.location
                    val gpsIsValid = gps.hasValidReading
                    demAltitude = DEM.getElevation(location)?.meters()?.distance ?: 0f
                    onMain {
                        if (gpsIsValid) {
                            notifyListeners()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DigitalElevationModel", "Unable to get DEM elevation", e)
                }
            }
        }
        return true
    }

    override fun startImpl() {
        onUpdate()
        gps.start(this::onUpdate)
    }

    override fun stopImpl() {
        gps.stop(this::onUpdate)
        queue.cancel()
        job?.cancel()
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
    override val fixTimeElapsedNanos: Long?
        get() = gps.fixTimeElapsedNanos
    override val time: Instant
        get() = gps.time
    override val speed: Speed
        get() = gps.speed
}