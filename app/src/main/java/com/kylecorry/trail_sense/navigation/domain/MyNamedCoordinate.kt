package com.kylecorry.trail_sense.navigation.domain

import android.os.Parcelable
import com.kylecorry.andromeda.core.system.GeoUriParser
import com.kylecorry.sol.units.Coordinate
import kotlinx.parcelize.Parcelize

@Parcelize
data class MyNamedCoordinate(
    val coordinate: Coordinate,
    val name: String? = null,
    val elevation: Float? = null
) : Parcelable {
    companion object {
        fun from(namedCoordinate: GeoUriParser.NamedCoordinate): MyNamedCoordinate {
            return MyNamedCoordinate(namedCoordinate.coordinate, namedCoordinate.name)
        }
    }
}