package com.kylecorry.trail_sense.navigation.infrastructure

import android.net.Uri
import com.kylecorry.trail_sense.shared.Coordinate

class GeoUriParser {

    fun parse(data: Uri): NamedCoordinate? {
        val pattern = "geo:([-\\d.]+),([-\\d.]+)(\\([^\\)]+\\))?(?:\\?[\\w=&]*q=([^&]+))?"
        val regex = Regex(pattern)
        val matches = regex.find(data.toString())
        println(data.toString())

        if (matches != null){
            val lat = matches.groupValues[1].toDouble()
            val lng = matches.groupValues[2].toDouble()
            val coord = Coordinate(lat, lng)
            val label = Uri.decode(matches.groupValues[3]).replace('+', ' ')
            val address = Uri.decode(matches.groupValues[4]).replace('+', ' ')

            val name = when {
                label.isNotEmpty() -> {
                    label
                }
                address.isNotEmpty() -> {
                    address
                }
                else -> {
                    null
                }
            }

            return NamedCoordinate(coord, name)
        }

        return null
    }


    data class NamedCoordinate(val coordinate: Coordinate, val name: String? = null)

}