package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

class LocationCopy(private val context: Context) : ILocationSender {

    override fun send(location: Coordinate) {
        val formatService = FormatService(context)
        val locString = formatService.formatLocation(location)
        Clipboard.copy(context, locString, context.getString(R.string.copied_to_clipboard_toast))
    }

}