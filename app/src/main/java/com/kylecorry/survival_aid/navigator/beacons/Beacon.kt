package com.kylecorry.survival_aid.navigator.beacons

import com.kylecorry.survival_aid.navigator.gps.Coordinate

data class Beacon(val name: String, val coordinate: Coordinate){
    companion object {
        const val DB_BEACON_TABLE = "beacons"
        const val DB_NAME = "name"
        const val DB_LAT = "lat"
        const val DB_LNG = "lng"
    }
}