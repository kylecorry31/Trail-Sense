package com.kylecorry.trail_sense.tools.backtrack.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.andromeda.signal.CellNetworkQuality
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.beacons.Beacon
import com.kylecorry.trail_sense.shared.beacons.BeaconOwner
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.paths.PathPoint
import com.kylecorry.trail_sense.shared.sensors.NullCellSignalSensor
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.altimeter.FusedAltimeter
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant

class BacktrackCommand(private val context: Context) : CoroutineCommand {

    private val prefs = UserPreferences(context)

    private val sensorService = SensorService(context)
    private val gps = sensorService.getGPS(true)
    private val altimeter = sensorService.getAltimeter(true)
    private val cellSignalSensor =
        if (prefs.backtrackSaveCellHistory) sensorService.getCellSignal(true) else NullCellSignalSensor()

    private val pathService = PathService.getInstance(context)
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

            val cell = cellSignalSensor.signals.maxByOrNull { it.strength }

            val waypoint = PathPoint(
                0,
                0,
                gps.location,
                if (shouldReadAltimeter()) altimeter.altitude else gps.altitude,
                Instant.now(),
                if (cell != null) CellNetworkQuality(cell.network, cell.quality) else null
            )

            pathService.addBacktrackPoint(waypoint)
            waypoint
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
                                    .first { it.id == point.cellSignal.network.id }
                            ),
                            formatService.formatQuality(point.cellSignal.quality)
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