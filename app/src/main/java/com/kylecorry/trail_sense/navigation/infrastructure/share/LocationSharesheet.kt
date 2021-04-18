package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils

class LocationSharesheet(private val context: Context) : ILocationSender {

    override fun send(location: Coordinate) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getShareString(location))
            type = "text/plain"
        }
        IntentUtils.openChooser(context, intent, context.getString(R.string.share_action_send))
    }

    private fun getShareString(locationCoordinate: Coordinate): String {
        val location = "${locationCoordinate.latitude}, ${locationCoordinate.longitude}"
        val locationUtm = locationCoordinate.toUTM()
        val osmUrl = "https://www.openstreetmap.org/#map=16/${locationCoordinate.latitude}/${locationCoordinate.longitude}"
        return "${location}\n\nUTM: ${locationUtm}\n\n${context.getString(R.string.maps)}: $osmUrl"
    }

}