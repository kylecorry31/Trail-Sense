package com.kylecorry.trail_sense.navigation.beacons.ui.form

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.science.geology.IGeologyService
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.colors.fromColor

data class CreateBeaconData(
    val id: Long = 0,
    val name: String = "",
    val coordinate: Coordinate? = null,
    val elevation: Distance? = null,
    val createAtDistance: Boolean = false,
    val distanceTo: Distance? = null,
    val bearingTo: Bearing? = null,
    val bearingIsTrueNorth: Boolean = false,
    val groupId: Long? = null,
    val color: AppColor = AppColor.Orange,
    val notes: String = "",
    val isVisible: Boolean = true
) {

    fun toBeacon(
        geology: IGeologyService = GeologyService(),
        isComplete: Specification<CreateBeaconData> = IsBeaconFormDataComplete()
    ): Beacon? {
        if (!isComplete.isSatisfiedBy(this)) return null

        val coordinate = if (createAtDistance) {
            val distanceTo = distanceTo?.meters()?.distance?.toDouble() ?: 0.0
            val bearingTo = bearingTo ?: Bearing.from(CompassDirection.North)
            val declination = if (!bearingIsTrueNorth) geology.getGeomagneticDeclination(
                coordinate!!,
                elevation?.meters()?.distance
            ) else 0f
            coordinate!!.plus(distanceTo, bearingTo.withDeclination(declination))
        } else {
            coordinate!!
        }

        return Beacon(
            id,
            name,
            coordinate,
            isVisible,
            notes,
            groupId,
            elevation?.meters()?.distance,
            color = color.color,
        )
    }

    companion object {
        val empty = CreateBeaconData()

        fun from(uri: GeoUri): CreateBeaconData {
            val name = uri.queryParameters.getOrDefault("label", "")
            val coordinate = uri.coordinate
            val elevation = uri.altitude ?: uri.queryParameters.getOrDefault(
                "ele",
                ""
            ).toFloatOrNull()
            val elevationDistance = elevation?.let { Distance.meters(elevation) }
            return CreateBeaconData(
                name = name,
                coordinate = coordinate,
                elevation = elevationDistance
            )
        }

        fun from(beacon: Beacon): CreateBeaconData {
            return CreateBeaconData(
                beacon.id,
                beacon.name,
                beacon.coordinate,
                beacon.elevation?.let { Distance.meters(it) },
                false,
                null,
                null,
                false,
                beacon.parentId,
                AppColor.values().fromColor(beacon.color) ?: AppColor.Orange,
                beacon.comment ?: "",
                beacon.visible
            )
        }

    }
}
