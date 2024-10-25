package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

interface IMappableBearing {
    val bearing: Bearing
    val color: Int
}

data class MappableBearing(override val bearing: Bearing, override val color: Int) :
    IMappableBearing

interface IMappableReferencePoint : Identifiable {
    val drawableId: Int
    val tint: Int?
    val opacity: Float
    val bearing: Bearing
    val rotation: Float
}

data class MappableReferencePoint(
    override val id: Long,
    override val drawableId: Int,
    override val bearing: Bearing,
    override val tint: Int? = null,
    override val opacity: Float = 1f,
    override val rotation: Float = 0f
) : IMappableReferencePoint


interface IMappableLocation : Identifiable {
    val coordinate: Coordinate
    val color: Int
    val icon: BeaconIcon?
    val elevation: Float?
}

data class MappableLocation(
    override val id: Long,
    override val coordinate: Coordinate,
    override val color: Int,
    override val icon: BeaconIcon?,
    override val elevation: Float? = null
) : IMappableLocation

interface IMappablePath : Identifiable {
    val points: List<IMappableLocation>
    val color: Int
    val style: LineStyle
    val name: String?
}

data class MappablePath(
    override val id: Long,
    override val points: List<IMappableLocation>,
    override val color: Int,
    override val style: LineStyle,
    override val name: String? = null
) : IMappablePath