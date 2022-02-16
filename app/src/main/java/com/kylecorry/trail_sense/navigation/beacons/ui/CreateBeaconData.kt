package com.kylecorry.trail_sense.navigation.beacons.ui

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.colors.fromColor

data class CreateBeaconData(
    val name: String?,
    val coordinate: Coordinate?,
    val elevation: Distance?,
    val createAtDistance: Boolean,
    val distanceTo: Distance?,
    val bearingTo: Bearing?,
    val groupId: Long?,
    val color: AppColor,
    val notes: String?
) {
    companion object {
        val empty =
            CreateBeaconData(null, null, null, false, null, null, null, AppColor.Orange, null)

        fun from(beacon: Beacon): CreateBeaconData {
            return CreateBeaconData(
                beacon.name,
                beacon.coordinate,
                beacon.elevation?.let { Distance.meters(it) },
                false,
                null,
                null,
                beacon.parentId,
                AppColor.values().fromColor(beacon.color) ?: AppColor.Orange,
                beacon.comment
            )
        }

    }
}
