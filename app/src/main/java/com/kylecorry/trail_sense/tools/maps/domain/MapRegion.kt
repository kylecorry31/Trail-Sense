package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.trailsensecore.domain.geo.Coordinate

data class MapRegion(val north: Double, val east: Double, val south: Double, val west: Double){
    fun contains(location: Coordinate): Boolean {
        val containsLatitude = location.latitude in south..north

        val containsLongitude = if (east < 0 && west > 0){
            location.longitude >= west || location.longitude <= east
        } else {
            location.longitude in west..east
        }

        return containsLatitude && containsLongitude
    }
}
