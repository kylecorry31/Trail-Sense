package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.shared.domain.Coordinate

class BeaconSharesheet(private val context: Context): IBeaconSender {
    override fun send(beacon: Beacon) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getOsmUrl(beacon.coordinate))
            // TODO: Share other details
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    private fun getOsmUrl(coordinate: Coordinate): String {
        return "https://www.openstreetmap.org/#map=16/${coordinate.latitude}/${coordinate.longitude}"
    }

}