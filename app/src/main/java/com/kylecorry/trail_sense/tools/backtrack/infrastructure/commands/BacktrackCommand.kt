package com.kylecorry.trail_sense.tools.backtrack.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.core.sensors.read
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.beacons.Beacon
import com.kylecorry.trail_sense.shared.beacons.BeaconOwner
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.paths.PathPoint
import com.kylecorry.trail_sense.shared.sensors.NullCellSignalSensor
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.altimeter.FusedAltimeter
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant

class BacktrackCommand(private val context: Context) : CoroutineCommand {

    private val prefs = UserPreferences(context)
    private val cache = Preferences(context)

    private val sensorService = SensorService(context)
    private val gps = sensorService.getGPS(true)
    private val altimeter = sensorService.getAltimeter(true)
    private val cellSignalSensor =
        if (prefs.backtrackSaveCellHistory) sensorService.getCellSignal(true) else NullCellSignalSensor()

    private val history = prefs.navigation.backtrackHistory

    private val waypointRepo = WaypointRepo.getInstance(context)
    private val beaconRepo = BeaconRepo.getInstance(context)
    private val formatService = FormatService(context)

    override suspend fun execute() {
        updateSensors()
        val point = recordWaypoint()
        createLastSignalBeacon(point)
    }


    private suspend fun updateSensors() {
        withContext(Dispatchers.IO) {
            try {
                withTimeoutOrNull(Duration.ofSeconds(10).toMillis()) {
                    val jobs = mutableListOf<Job>()
                    jobs.add(launch { gps.read() })

                    if (shouldReadAltimeter()) {
                        jobs.add(launch { altimeter.read() })
                    }

                    jobs.add(launch { cellSignalSensor.read() })

                    jobs.joinAll()
                }
            } finally {
                gps.stop(null)
                altimeter.stop(null)
                cellSignalSensor.stop(null)
            }
        }
    }

    private fun shouldReadAltimeter(): Boolean {
        return altimeter !is IGPS && altimeter !is FusedAltimeter
    }

    private suspend fun recordWaypoint(): PathPoint {
        return withContext(Dispatchers.IO) {
            val pathId = cache.getLong(context.getString(R.string.pref_last_backtrack_path_id))
                ?: ((waypointRepo.getLastPathId() ?: 0L) + 1)

            cache.putLong(context.getString(R.string.pref_last_backtrack_path_id), pathId)

            val cell = cellSignalSensor.signals.maxByOrNull { it.strength }
            val waypoint = WaypointEntity(
                gps.location.latitude,
                gps.location.longitude,
                if (shouldReadAltimeter()) altimeter.altitude else gps.altitude,
                Instant.now().toEpochMilli(),
                cell?.network?.id,
                cell?.quality?.ordinal,
                pathId
            )

            waypointRepo.addWaypoint(waypoint)
            waypointRepo.deleteOlderThan(Instant.now().minus(history))
            waypoint.toPathPoint()
        }
    }

    private suspend fun createLastSignalBeacon(point: PathPoint) {
        if (point.cellSignal == null) {
            return
        }
        withContext(Dispatchers.IO) {
            val existing = beaconRepo.getTemporaryBeacon(BeaconOwner.CellSignal)
            beaconRepo.addBeacon(
                BeaconEntity.from(
                    Beacon(
                        existing?.id ?: 0L,
                        context.getString(
                            R.string.last_signal_beacon_name,
                            formatService.formatCellNetwork(
                                CellNetwork.values()
                                    .first { it.id == point.cellSignal!!.network.id }
                            ),
                            formatService.formatQuality(point.cellSignal!!.quality)
                        ),
                        point.coordinate,
                        false,
                        elevation = point.elevation,
                        temporary = true,
                        owner = BeaconOwner.CellSignal,
                        color = AppColor.Orange.color
                    )
                )
            )
        }
    }

}