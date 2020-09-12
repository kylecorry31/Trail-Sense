package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.infrastructure.persistence.Clipboard

class BeaconCoordinatesCopy(private val context: Context, private val clipboard: Clipboard, private val prefs: UserPreferences) :
    IBeaconSender {

    override fun send(beacon: Beacon) {
        val locString = prefs.navigation.formatLocation(beacon.coordinate)
        clipboard.copy(locString, context.getString(R.string.copied_to_clipboard_toast))
    }

}