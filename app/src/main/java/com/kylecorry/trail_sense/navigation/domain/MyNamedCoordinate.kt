package com.kylecorry.trail_sense.navigation.domain

import android.os.Parcelable
import com.kylecorry.andromeda.core.system.GeoUriParser
import com.kylecorry.andromeda.core.units.Coordinate
import kotlinx.parcelize.Parcelize

@Parcelize
data class MyNamedCoordinate(val coordinate: Coordinate, val name: String? = null) : Parcelable {
    companion object {
        fun from(namedCoordinate: GeoUriParser.NamedCoordinate): MyNamedCoordinate {
            return MyNamedCoordinate(namedCoordinate.coordinate, namedCoordinate.name)
        }
    }
}