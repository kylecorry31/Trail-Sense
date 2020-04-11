package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.kylecorry.trail_sense.shared.Coordinate

class LocationClipboard(private val context: Context) : ILocationSender {

    override fun send(location: Coordinate) {
        val locString = location.toString()
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(locString, locString))
    }

}