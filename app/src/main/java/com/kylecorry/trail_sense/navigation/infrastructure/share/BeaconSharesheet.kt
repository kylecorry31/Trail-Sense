package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.units.CoordinateExtensions.toDecimalDegrees
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.beacons.Beacon
import com.kylecorry.trail_sense.shared.sharing.MapSiteService

class BeaconSharesheet(private val context: Context) : IBeaconSender {

    private val prefs by lazy { UserPreferences(context) }
    private val formatService by lazy { FormatService(context) }
    private val mapService = MapSiteService()

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

    private fun getShareText(beacon: Beacon): String {
        val name = beacon.name
        val coordinate = beacon.coordinate
        val mapUrl = mapService.getUrl(coordinate, prefs.mapSite)
        val coordinateString = getCoordinateLatLon(coordinate)
        val coordinateUser = formatService.formatLocation(coordinate)
        return "${name}\n\n${coordinateString}\n\n${formatService.formatCoordinateType(prefs.navigation.coordinateFormat)}: ${coordinateUser}\n\n${
            context.getString(
                R.string.maps
            )
        }: $mapUrl"
    }

    private fun getCoordinateLatLon(coordinate: Coordinate): String {
        return coordinate.toDecimalDegrees()
    }

}