package com.kylecorry.trail_sense.navigation.beacons.infrastructure.export

import android.content.Context
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconEntity
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconGroupEntity
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon

class BeaconGpxImporter(private val context: Context) {

    private val repo by lazy { BeaconRepo.getInstance(context) }
    private val formatService by lazy { FormatService(context) }

    suspend fun import(gpx: GPXData): Int {
        val waypoints = gpx.waypoints
        val groupNames = waypoints.mapNotNull { it.group }.distinct()

        val groupIdMap = mutableMapOf<String, Long>()
        groupNames.forEach {
            val id = repo.addBeaconGroup(BeaconGroupEntity(it).also { it.id = 0 })
            groupIdMap[it] = id
        }

        val beacons = waypoints.map {
            val name = it.name
                ?: (if (it.time != null) formatService.formatDateTime(it.time!!.toZonedDateTime()) else null)
                ?: formatService.formatLocation(it.coordinate)
            Beacon(
                0,
                name,
                it.coordinate,
                comment = it.comment,
                elevation = it.elevation,
                parent = if (it.group != null) groupIdMap[it.group] else null,
                color = AppColor.Orange.color
            )
        }

        beacons.forEach {
            repo.addBeacon(BeaconEntity.from(it))
        }

        return waypoints.size
    }

}