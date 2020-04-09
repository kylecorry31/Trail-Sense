package com.kylecorry.trail_sense.shared

import kotlin.math.abs
import kotlin.math.roundToInt

data class Coordinate(val latitude: Double, val longitude: Double){

    private val latitudeDMS: String
        get() {
            val direction = if (latitude < 0) "S" else "N"
            return "${dmsString(latitude)} $direction"
        }

    private val longitudeDMS: String
        get() {
            val direction = if (longitude < 0) "W" else "E"
            return "${dmsString(longitude)} $direction"
        }

    override fun toString(): String {
        return "$latitudeDMS    $longitudeDMS"
    }

    private fun dmsString(degrees: Double): String {
        val deg = abs(degrees.toInt())
        val minutes = abs((degrees % 1) * 60)
        val seconds = abs(((minutes % 1) * 60).roundToInt())
        return "$deg°${minutes.toInt()}'$seconds\""
    }
}