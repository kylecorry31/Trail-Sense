package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.shared.domain.Coordinate

class LocationClipboard(private val context: Context) : ILocationSender {

    override fun send(location: Coordinate) {
        val locString = location.toString()
        val cm = context.getSystemService<ClipboardManager>()
        cm?.setPrimaryClip(ClipData.newPlainText(locString, locString))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

}