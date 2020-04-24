package com.kylecorry.trail_sense.shared

import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import java.util.*
import kotlin.math.abs

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

    fun distanceTo(coordinate: Coordinate): Float {
        val service = NavigationService()
        return service.navigate(this, coordinate).distance
    }

    override fun toString(): String {
        return "$latitudeDMS, $longitudeDMS"
    }

    fun getFormattedString(): String {
        return "$latitudeDMS    $longitudeDMS"
    }

    private fun dmsString(degrees: Double): String {
        val deg = abs(degrees.toInt())
        val minutes = abs((degrees % 1) * 60)
        val seconds = abs(((minutes % 1) * 60).roundPlaces(1))
        return "$deg°${minutes.toInt()}'$seconds\""
    }

    companion object {
        fun degreeMinutesSeconds(latitude: String, longitude: String): Coordinate? {
            val dmsRegex = Regex("(\\d+)°(\\d+)'([\\d.]+)\"\\s*([wWeEnNsS])")
            var latitudeDecimal = 0.0
            var longitudeDecimal = 0.0
            val lat = dmsRegex.find(latitude)
            val lon = dmsRegex.find(longitude)
            if (lat == null || lon == null){
                return null
            }
            latitudeDecimal += lat.groupValues[1].toDouble()
            latitudeDecimal += lat.groupValues[2].toDouble() / 60
            latitudeDecimal += lat.groupValues[3].toDouble() / (60 * 60)
            latitudeDecimal *= if (lat.groupValues[4].toLowerCase(Locale.getDefault()) == "n") 1 else -1

            longitudeDecimal += lon.groupValues[1].toDouble()
            longitudeDecimal += lon.groupValues[2].toDouble() / 60
            longitudeDecimal += lon.groupValues[3].toDouble() / (60 * 60)
            longitudeDecimal *= if (lon.groupValues[4].toLowerCase(Locale.getDefault()) == "e") 1 else -1

            return Coordinate(latitudeDecimal, longitudeDecimal)
        }

        fun degreeDecimalMinutes(latitude: String, longitude: String): Coordinate? {
            val regex = Regex("(\\d+)°([\\d.]+)'\\s*([wWeEnNsS])")
            var latitudeDecimal = 0.0
            var longitudeDecimal = 0.0
            val lat = regex.find(latitude)
            val lon = regex.find(longitude)
            if (lat == null || lon == null){
                return null
            }
            latitudeDecimal += lat.groupValues[1].toDouble()
            latitudeDecimal += lat.groupValues[2].toDouble() / 60
            latitudeDecimal *= if (lat.groupValues[3].toLowerCase(Locale.getDefault()) == "n") 1 else -1

            longitudeDecimal += lon.groupValues[1].toDouble()
            longitudeDecimal += lon.groupValues[2].toDouble() / 60
            longitudeDecimal *= if (lon.groupValues[3].toLowerCase(Locale.getDefault()) == "e") 1 else -1

            return Coordinate(latitudeDecimal, longitudeDecimal)
        }
    }
}