package com.kylecorry.trail_sense.tools.paths.infrastructure.commands

import android.content.Context
import android.os.SystemClock
import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.networkQuality
import com.kylecorry.trail_sense.shared.safeRoundPlaces
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.MockCellSignalSensor
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.age
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import java.time.Instant

class BacktrackCommand(
    private val context: Context,
    private val pathId: Long = 0,
    private val alerter: IValueAlerter<Distance> = BacktrackAlerter(context)
) : CoroutineCommand {

    private val prefs = UserPreferences(context)

    private val sensorService = SensorService(context)
    private val gps = sensorService.getGPS(SensorService.SINGLE_FIX_GPS_FREQUENCY)
    private val altimeter = sensorService.getAltimeter(gps = gps)
    private val cellSignalSensor =
        if (prefs.backtrackSaveCellHistory && pathId == 0L) sensorService.getCellSignal() else MockCellSignalSensor()

    private val pathService = PathService.getInstance(context)

    override suspend fun execute() = onDefault {
        val start = SystemClock.elapsedRealtime()
        updateSensors()
        val point = recordWaypoint()
        getAppService<Logger>().info(
            TAG,
            "Recorded ${if (pathId == 0L) "backtrack" else "path $pathId"} point in ${SystemClock.elapsedRealtime() - start}ms " +
                "(GPS: ${gps.hasValidReading}, Altitude: ${altimeter.hasValidReading}, " +
                "Cell Signal: ${if (cellSignalSensor is MockCellSignalSensor) "N/A" else cellSignalSensor.hasValidReading})"
        )
        CreateLastSignalBeaconCommand(context).execute(point)
        showNotification()
    }

    private suspend fun showNotification() {
        if (pathId != 0L) {
            return
        }
        val backtrackId = pathService.getBacktrackPathId() ?: return
        val backtrackPath = pathService.getPath(backtrackId) ?: return

        val distance = backtrackPath.metadata.distance
        alerter.alert(distance)
    }

    private suspend fun updateSensors() {
        readAll(
            listOf(gps, altimeter, cellSignalSensor),
            timeout = SensorService.GPS_READ_TIMEOUT,
            forceStopOnCompletion = true
        )

        if (!gps.hasValidReading || (gps as? CustomGPS)?.isTimedOut == true) {
            val reason = if (gps.hasValidReading) "timed out" else "no valid reading"
            val path = if (pathId == 0L) "backtrack" else "path $pathId"
            getAppService<Logger>().warn(
                TAG,
                "GPS did not receive a fix ($reason), recording $path point from a reading " +
                    "${gps.age(SystemTimeProvider()).seconds}s old with ${gps.horizontalAccuracy?.safeRoundPlaces(1)}m accuracy"
            )
        }
    }


    private suspend fun recordWaypoint(): PathPoint {
        return onIO {
            val waypoint = PathPoint(
                0,
                pathId,
                gps.location,
                altimeter.altitude,
                Instant.now(),
                cellSignalSensor.networkQuality()
            )

            if (pathId == 0L) {
                pathService.addBacktrackPoint(waypoint)
            } else {
                pathService.addWaypoint(waypoint)
            }
            waypoint
        }
    }

    companion object {
        private const val TAG = "BacktrackCommand"
    }

}
