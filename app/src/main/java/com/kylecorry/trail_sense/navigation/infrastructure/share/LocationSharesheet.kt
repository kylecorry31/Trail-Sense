package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.CoordinateFormat
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils

class LocationSharesheet(private val context: Context) : ILocationSender {

    private val geoService = GeoService()
    private val prefs by lazy { UserPreferences(context) }
    private val formatService by lazy { FormatServiceV2(context) }

    override fun send(location: Coordinate) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getShareString(location))
            type = "text/plain"
        }
        IntentUtils.openChooser(context, intent, context.getString(R.string.share_action_send))
    }

    private fun getShareString(coordinate: Coordinate): String {
        val location = formatService.formatLocation(coordinate, CoordinateFormat.DecimalDegrees)
        val mapUrl = geoService.getMapUrl(coordinate, prefs.mapSite)

        if (prefs.navigation.coordinateFormat == CoordinateFormat.DecimalDegrees){
            return "${location}\n\n${
                context.getString(
                    R.string.maps
                )
            }: $mapUrl"
        }

        val coordinateUser = formatService.formatLocation(coordinate)
        return "${location}\n\n${formatService.formatCoordinateType(prefs.navigation.coordinateFormat)}: ${coordinateUser}\n\n${
            context.getString(
                R.string.maps
            )
        }: $mapUrl"
    }

}