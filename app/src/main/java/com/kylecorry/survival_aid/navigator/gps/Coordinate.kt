package com.kylecorry.survival_aid.navigator.gps

data class Coordinate(val latitude: Double, val longitude: Double){
    override fun toString(): String {
        return "$latitude,  $longitude"
    }
}