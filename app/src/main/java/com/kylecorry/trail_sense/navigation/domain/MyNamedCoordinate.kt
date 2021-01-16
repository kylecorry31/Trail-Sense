package com.kylecorry.trail_sense.navigation.domain

import android.os.Parcelable
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.system.GeoUriParser
import kotlinx.parcelize.Parcelize

@Parcelize
data class MyNamedCoordinate(val coordinate: Coordinate, val name: String? = null) : Parcelable {
    companion object {
        fun from(namedCoordinate: GeoUriParser.NamedCoordinate): MyNamedCoordinate {
            return MyNamedCoordinate(namedCoordinate.coordinate, namedCoordinate.name)
        }
    }
}