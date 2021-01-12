package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.persistence.Clipboard

class LocationCopy(private val context: Context, private val clipboard: Clipboard, private val prefs: UserPreferences) :
    ILocationSender {

    override fun send(location: Coordinate) {
        val locString = prefs.navigation.locationFormatter.format(location)
        clipboard.copy(locString, context.getString(R.string.copied_to_clipboard_toast))
    }

}