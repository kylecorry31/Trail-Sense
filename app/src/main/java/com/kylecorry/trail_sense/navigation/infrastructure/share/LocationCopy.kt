package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import com.kylecorry.andromeda.clipboard.IClipboard
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trailsensecore.domain.geo.Coordinate

class LocationCopy(private val context: Context, private val clipboard: IClipboard) :
    ILocationSender {

    override fun send(location: Coordinate) {
        val formatService = FormatServiceV2(context)
        val locString = formatService.formatLocation(location)
        clipboard.copy(locString, context.getString(R.string.copied_to_clipboard_toast))
    }

}