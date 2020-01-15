package com.kylecorry.trail_sense.models

data class Coordinate(val latitude: Double, val longitude: Double){
    override fun toString(): String {
        return "$latitude,  $longitude"
    }
}