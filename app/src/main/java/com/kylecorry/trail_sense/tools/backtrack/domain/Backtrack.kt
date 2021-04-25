package com.kylecorry.trail_sense.tools.backtrack.domain

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.IBeaconRepo
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.IWaypointRepo
import com.kylecorry.trailsensecore.domain.geo.PathPoint
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconOwner
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.sensors.network.ICellSignalSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.read
import com.kylecorry.trailsensecore.infrastructure.system.PermissionUtils
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant

class Backtrack(
    private val context: Context,
    private val gps: IGPS,
    private val cellSignalSensor: ICellSignalSensor,
    private val backtrackRepo: IWaypointRepo,
    private val beaconRepo: IBeaconRepo,
    private val recordCellSignal: Boolean = true
) {

    private val formatService by lazy { FormatServiceV2(context) }

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
                            CellSignalUtils.getCellTypeString(
                                context,
                                point.cellSignal!!.network
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

    private suspend fun updateSensors(){
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(Duration.ofSeconds(30).toMillis()) {
                val jobs = mutableListOf<Job>()
                jobs.add(launch { gps.read() })

                if (recordCellSignal && PermissionUtils.isBackgroundLocationEnabled(context)) {
                    jobs.add(launch { cellSignalSensor.read() })
                }

                jobs.joinAll()
            }
        }
    }

    private suspend fun recordWaypoint(): PathPoint {
        return withContext(Dispatchers.IO) {
            val cell = cellSignalSensor.signals.maxByOrNull { it.strength }
            val waypoint = WaypointEntity(
                gps.location.latitude,
                gps.location.longitude,
                gps.altitude,
                Instant.now().toEpochMilli(),
                cell?.network?.id,
                cell?.quality?.ordinal,
            )

            backtrackRepo.addWaypoint(waypoint)
            backtrackRepo.deleteOlderThan(Instant.now().minus(Duration.ofDays(2)))
            waypoint.toPathPoint()
        }
    }

}