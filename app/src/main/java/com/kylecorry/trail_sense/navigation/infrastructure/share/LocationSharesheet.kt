package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import android.content.Intent
import com.kylecorry.trailsensecore.domain.geo.Coordinate

class LocationSharesheet(private val context: Context): ILocationSender {
    override fun send(location: Coordinate) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getOsmUrl(location))
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    private fun getOsmUrl(coordinate: Coordinate): String {
        return "https://www.openstreetmap.org/#map=16/${coordinate.latitude}/${coordinate.longitude}"
    }

}