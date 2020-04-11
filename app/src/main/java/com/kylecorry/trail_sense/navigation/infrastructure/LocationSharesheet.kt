package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.shared.Coordinate

class LocationSharesheet(private val context: Context): ILocationSender {
    override fun send(location: Coordinate) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, location.toString())
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }
}