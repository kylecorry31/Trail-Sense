package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.navigation.Beacon

class BeaconSharesheet(private val context: Context): IBeaconSender {

    private val geoService = GeoService()
    private val prefs by lazy { UserPreferences(context) }

    override fun send(beacon: Beacon) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getShareText(beacon))
            // TODO: Share other details
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    private fun getShareText(beacon: Beacon):String {
        val name = beacon.name
        val coordinate = beacon.coordinate
        val mapUrl = geoService.getMapUrl(coordinate, prefs.mapSite)
        val coordinateString = getCoordinateLatLon(coordinate)
        val coordinateUtm = getCoordinateUtm(coordinate)
        return "${name}\n\n${coordinateString}\n\n${context.getString(R.string.coordinate_format_utm)}: ${coordinateUtm}\n\n${context.getString(R.string.maps)}: $mapUrl"
    }

    private fun getCoordinateLatLon(coordinate: Coordinate): String {
        return coordinate.toDecimalDegrees()
    }

    private fun getCoordinateUtm(coordinate: Coordinate): String {
        return coordinate.toUTM()
    }

}