package com.kylecorry.trail_sense.navigation.beacons.infrastructure.export

import android.content.Context
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor

class BeaconGpxImporter(private val context: Context) {

    private val service by lazy { BeaconService(context) }
    private val formatService by lazy { FormatService.getInstance(context) }

    suspend fun import(gpx: GPXData, parent: Long? = null): Int {
        val waypoints = gpx.waypoints
        val groupNames = waypoints.mapNotNull { it.group }.distinct()

        val groupIdMap = mutableMapOf<String, Long>()
        groupNames.forEach {
            val id = service.add(BeaconGroup(0, it, parent))
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
                parentId = if (it.group != null) groupIdMap[it.group] else parent,
                color = AppColor.Orange.color
            )
        }

        beacons.forEach {
            service.add(it)
        }

        return waypoints.size
    }

}