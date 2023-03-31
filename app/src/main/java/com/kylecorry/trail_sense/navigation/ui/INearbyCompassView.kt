package com.kylecorry.trail_sense.navigation.ui

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.navigation.paths.domain.LineStyle
import com.kylecorry.trail_sense.shared.database.Identifiable

interface INearbyCompassView {
    var azimuth: Bearing
    fun setLocation(location: Coordinate)
    fun setDeclination(declination: Float)
    fun highlightLocation(location: IMappableLocation?) // Remove these
    fun showReferences(references: List<IMappableReferencePoint>) // Use this to show beacons on non-radar compass
    fun showDirection(bearing: IMappableBearing?)
}

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
}

data class MappableReferencePoint(
    override val id: Long,
    override val drawableId: Int,
    override val bearing: Bearing,
    override val tint: Int? = null,
    override val opacity: Float = 1f,
) : IMappableReferencePoint


interface IMappableLocation : Identifiable {
    val coordinate: Coordinate
    val color: Int
    val icon: BeaconIcon?
}

data class MappableLocation(
    override val id: Long,
    override val coordinate: Coordinate,
    override val color: Int,
    override val icon: BeaconIcon?
) : IMappableLocation

interface IMappablePath : Identifiable {
    val points: List<IMappableLocation>
    val color: Int
    val style: LineStyle
}

data class MappablePath(
    override val id: Long,
    override val points: List<IMappableLocation>,
    override val color: Int,
    override val style: LineStyle
) : IMappablePath