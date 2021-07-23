package com.kylecorry.trail_sense.navigation.infrastructure.export

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.domain.BeaconGroupEntity
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trailsensecore.domain.time.toZonedDateTime
import com.kylecorry.trailsensecore.infrastructure.gpx.GPXParser
import com.kylecorry.trailsensecore.infrastructure.gpx.GPXWaypoint

class BeaconIOService(private val context: Context) {

    private val repo by lazy { BeaconRepo.getInstance(context) }
    private val formatService by lazy { FormatServiceV2(context) }

    fun export(beacons: List<Beacon>, groups: List<BeaconGroup>): String {
        val groupNames = mutableMapOf<Long, String>()
        for (group in groups) {
            groupNames[group.id] = group.name
        }

        val waypoints = beacons.map {
            GPXWaypoint(
                it.coordinate,
                it.name,
                it.elevation,
                it.comment,
                null,
                if (it.beaconGroupId == null) null else groupNames[it.beaconGroupId]
            )
        }

        return GPXParser().toGPX(waypoints, context.getString(R.string.app_name))
    }

    fun getGPXWaypoints(gpx: String): List<GPXWaypoint> {
        return GPXParser().getWaypoints(gpx)
    }

    suspend fun import(waypoints: List<GPXWaypoint>): Int {
        val groupNames = waypoints.mapNotNull { it.group }.distinct()

        val groupIdMap = mutableMapOf<String, Long>()
        groupNames.forEach {
            val id = repo.addBeaconGroup(BeaconGroupEntity(it).also { it.id = 0 })
            groupIdMap[it] = id
        }

        val beacons = waypoints.map {
            val name = it.name ?:
                (if (it.time != null) formatService.formatDateTime(it.time!!.toZonedDateTime()) else null) ?:
                formatService.formatLocation(it.coordinate)
            Beacon(
                0,
                name,
                it.coordinate,
                comment = it.comment,
                elevation = it.elevation,
                beaconGroupId = if (it.group != null) groupIdMap[it.group] else null,
                color = AppColor.Orange.color
            )
        }

        beacons.forEach {
            repo.addBeacon(BeaconEntity.from(it))
        }

        return waypoints.size
    }

}