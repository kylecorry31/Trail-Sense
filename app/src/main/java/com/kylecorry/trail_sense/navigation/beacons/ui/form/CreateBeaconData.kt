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
    val id: Long,
    val name: String?,
    val coordinate: Coordinate?,
    val elevation: Distance?,
    val createAtDistance: Boolean,
    val distanceTo: Distance?,
    val bearingTo: Bearing?,
    val groupId: Long?,
    val color: AppColor,
    val notes: String?,
    val isVisible: Boolean
) {

    fun toBeacon(
        geology: IGeologyService = GeologyService(),
        isComplete: Specification<CreateBeaconData> = IsBeaconFormDataComplete()
    ): Beacon? {
        if (!isComplete.isSatisfiedBy(this)) return null

        val coordinate = if (createAtDistance) {
            val distanceTo = distanceTo?.meters()?.distance?.toDouble() ?: 0.0
            val bearingTo = bearingTo ?: Bearing.from(CompassDirection.North)
            val declination = geology.getGeomagneticDeclination(
                coordinate!!,
                elevation?.meters()?.distance
            )
            coordinate.plus(distanceTo, bearingTo.withDeclination(declination))
        } else {
            coordinate!!
        }

        return Beacon(
            id,
            name!!,
            coordinate,
            isVisible,
            notes,
            groupId,
            elevation?.meters()?.distance,
            color = color.color,
        )
    }

    companion object {
        val empty =
            CreateBeaconData(
                0,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                AppColor.Orange,
                null,
                true
            )

        fun from(uri: GeoUri): CreateBeaconData {
            val name = uri.queryParameters.getOrDefault("label", null)
            val coordinate = uri.coordinate
            val elevation = uri.altitude ?: uri.queryParameters.getOrDefault(
                "ele",
                ""
            ).toFloatOrNull()
            val elevationDistance = elevation?.let { Distance.meters(elevation) }
            return CreateBeaconData(
                0,
                name,
                coordinate,
                elevationDistance,
                false,
                null,
                null,
                null,
                AppColor.Orange,
                null,
                true
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
                beacon.parentId,
                AppColor.values().fromColor(beacon.color) ?: AppColor.Orange,
                beacon.comment,
                beacon.visible
            )
        }

    }
}
