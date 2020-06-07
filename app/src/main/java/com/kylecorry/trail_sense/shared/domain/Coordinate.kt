package com.kylecorry.trail_sense.shared.domain

import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.shared.roundPlaces
import java.util.*
import kotlin.math.abs
import kotlin.math.absoluteValue

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

        fun parseLatitude(latitude: String): Double? {

            val dms =
                parseDMS(
                    latitude,
                    true
                )
            if (dms != null){
                return dms
            }

            val ddm =
                parseDDM(
                    latitude,
                    true
                )
            if (ddm != null){
                return ddm
            }

            return parseDecimal(
                latitude,
                true
            )
        }

        fun parseLongitude(longitude: String): Double? {

            val dms =
                parseDMS(
                    longitude,
                    false
                )
            if (dms != null){
                return dms
            }

            val ddm =
                parseDDM(
                    longitude,
                    false
                )
            if (ddm != null){
                return ddm
            }

            return parseDecimal(
                longitude,
                false
            )
        }

        fun degreeMinutesSeconds(latitude: String, longitude: String): Coordinate? {
            val latitudeDecimal =
                parseDMS(
                    latitude,
                    true
                )
            val longitudeDecimal =
                parseDMS(
                    longitude,
                    false
                )
            if (latitudeDecimal == null || longitudeDecimal == null){
                return null
            }
            return Coordinate(
                latitudeDecimal,
                longitudeDecimal
            )
        }

        fun degreeDecimalMinutes(latitude: String, longitude: String): Coordinate? {
            val latitudeDecimal =
                parseDDM(
                    latitude,
                    true
                )
            val longitudeDecimal =
                parseDDM(
                    longitude,
                    false
                )
            if (latitudeDecimal == null || longitudeDecimal == null){
                return null
            }
            return Coordinate(
                latitudeDecimal,
                longitudeDecimal
            )
        }

        private fun parseDecimal(latOrLng: String, isLatitude: Boolean): Double ? {
            val number = latOrLng.toDoubleOrNull() ?: return null

            return if (isLatitude && isValidLatitude(
                    number
                )
            ){
                number
            } else if(!isLatitude && isValidLongitude(
                    number
                )
            ) {
                number
            } else {
                null
            }
        }

        private fun parseDMS(latOrLng: String, isLatitude: Boolean): Double? {
            val dmsRegex = if (isLatitude) {
                Regex("(\\d+)°(\\d+)'([\\d.]+)\"\\s*([nNsS])")
            } else {
                Regex("(\\d+)°(\\d+)'([\\d.]+)\"\\s*([wWeE])")
            }
            val matches = dmsRegex.find(latOrLng) ?: return null

            var decimal = 0.0
            decimal += matches.groupValues[1].toDouble()
            decimal += matches.groupValues[2].toDouble() / 60
            decimal += matches.groupValues[3].toDouble() / (60 * 60)
            decimal *= if (isLatitude) {
                if (matches.groupValues[4].toLowerCase(Locale.getDefault()) == "n") 1 else -1
            } else {
                if (matches.groupValues[4].toLowerCase(Locale.getDefault()) == "e") 1 else -1
            }

            return if (isLatitude && isValidLatitude(
                    decimal
                )
            ){
                decimal
            } else if(!isLatitude && isValidLongitude(
                    decimal
                )
            ) {
                decimal
            } else {
                null
            }
        }

        private fun parseDDM(latOrLng: String, isLatitude: Boolean): Double? {
            val dmsRegex = if (isLatitude) {
                Regex("(\\d+)°([\\d.]+)'\\s*([nNsS])")
            } else {
                Regex("(\\d+)°([\\d.]+)'\\s*([wWeE])")
            }
            val matches = dmsRegex.find(latOrLng) ?: return null

            var decimal = 0.0
            decimal += matches.groupValues[1].toDouble()
            decimal += matches.groupValues[2].toDouble() / 60
            decimal *= if (isLatitude) {
                if (matches.groupValues[3].toLowerCase(Locale.getDefault()) == "n") 1 else -1
            } else {
                if (matches.groupValues[3].toLowerCase(Locale.getDefault()) == "e") 1 else -1
            }

            return if (isLatitude && isValidLatitude(
                    decimal
                )
            ){
                decimal
            } else if(!isLatitude && isValidLongitude(
                    decimal
                )
            ) {
                decimal
            } else {
                null
            }
        }

        private fun isValidLongitude(longitude: Double): Boolean {
            return longitude.absoluteValue <= 180
        }

        private fun isValidLatitude(latitude: Double): Boolean {
            return latitude.absoluteValue <= 90
        }
    }
}