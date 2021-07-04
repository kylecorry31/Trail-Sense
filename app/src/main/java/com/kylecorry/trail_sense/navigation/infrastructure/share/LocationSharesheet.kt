package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils

class LocationSharesheet(private val context: Context) : ILocationSender {

    private val geoService = GeoService()
    private val prefs by lazy { UserPreferences(context) }

    override fun send(location: Coordinate) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getShareString(location))
            type = "text/plain"
        }
        IntentUtils.openChooser(context, intent, context.getString(R.string.share_action_send))
    }

    private fun getShareString(locationCoordinate: Coordinate): String {
        val location = locationCoordinate.toDecimalDegrees()
        val locationUtm = locationCoordinate.toUTM()
        val mapUrl = geoService.getMapUrl(locationCoordinate, prefs.mapSite)
        return "${location}\n\n${context.getString(R.string.coordinate_format_utm)}: ${locationUtm}\n\n${context.getString(R.string.maps)}: $mapUrl"
    }

}