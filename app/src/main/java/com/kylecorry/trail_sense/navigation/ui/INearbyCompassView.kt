package com.kylecorry.trail_sense.navigation.ui

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.paths.domain.LineStyle
import com.kylecorry.trail_sense.shared.database.Identifiable

interface INearbyCompassView {
    fun setAzimuth(azimuth: Bearing)
    fun setLocation(location: Coordinate)
    fun setDeclination(declination: Float)
    fun showLocations(locations: List<IMappableLocation>)
    fun highlightLocation(location: IMappableLocation?)
    fun showPaths(paths: List<IMappablePath>)
    fun showReferences(references: List<IMappableReferencePoint>)
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
}

data class MappableLocation(
    override val id: Long,
    override val coordinate: Coordinate,
    override val color: Int
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