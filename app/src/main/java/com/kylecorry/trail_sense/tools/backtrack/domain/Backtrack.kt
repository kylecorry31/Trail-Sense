package com.kylecorry.trail_sense.tools.backtrack.domain

import android.content.Context
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.sensors.read
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.IBeaconRepo
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.altimeter.FusedAltimeter
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.IWaypointRepo
import com.kylecorry.trailsensecore.domain.geo.PathPoint
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconOwner
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant

class Backtrack(
    private val context: Context,
    private val gps: IGPS,
    private val altimeter: IAltimeter,
    private val cellSignalSensor: ICellSignalSensor,
    private val backtrackRepo: IWaypointRepo,
    private val beaconRepo: IBeaconRepo,
    private val recordCellSignal: Boolean = true,
    private val history: Duration = Duration.ofDays(2),
    private val cache: Preferences
) {

    private val formatService by lazy { FormatService(context) }

    suspend fun recordLocation() {
        updateSensors()
        val point = recordWaypoint()
        createLastSignalBeacon(point)
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

    private suspend fun updateSensors() {
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(Duration.ofSeconds(30).toMillis()) {
                val jobs = mutableListOf<Job>()
                jobs.add(launch { gps.read() })

                if (shouldReadAltimeter()) {
                    jobs.add(launch { altimeter.read() })
                }

                if (recordCellSignal && Permissions.isBackgroundLocationEnabled(context)) {
                    jobs.add(launch { cellSignalSensor.read() })
                }

                jobs.joinAll()
            }
        }
    }

    private fun shouldReadAltimeter(): Boolean {
        return altimeter !is IGPS && altimeter !is FusedAltimeter
    }

    private suspend fun recordWaypoint(): PathPoint {
        return withContext(Dispatchers.IO) {
            val pathId = cache.getLong(context.getString(R.string.pref_last_backtrack_path_id))
                ?: ((backtrackRepo.getLastPathId() ?: 0L) + 1)

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

            backtrackRepo.addWaypoint(waypoint)
            backtrackRepo.deleteOlderThan(Instant.now().minus(history))
            waypoint.toPathPoint()
        }
    }

}