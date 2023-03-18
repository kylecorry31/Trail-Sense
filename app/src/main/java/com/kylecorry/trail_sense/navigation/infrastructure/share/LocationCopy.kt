package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.units.CoordinateFormat
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

class LocationCopy(private val context: Context) : ILocationSender {

    override fun send(location: Coordinate, format: CoordinateFormat?) {
        val formatService = FormatService.getInstance(context)
        val locString = formatService.formatLocation(location, format)
        Clipboard.copy(context, locString, context.getString(R.string.copied_to_clipboard_toast))
    }

}