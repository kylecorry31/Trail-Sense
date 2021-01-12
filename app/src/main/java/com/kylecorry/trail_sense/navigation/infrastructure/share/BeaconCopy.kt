package com.kylecorry.trail_sense.navigation.infrastructure.share

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.infrastructure.persistence.Clipboard

class BeaconCopy(private val context: Context, private val clipboard: Clipboard, private val prefs: UserPreferences) :
    IBeaconSender {

    override fun send(beacon: Beacon) {
        val text = "${beacon.name}: ${prefs.navigation.locationFormatter.format(beacon.coordinate)}"
        clipboard.copy(text, context.getString(R.string.copied_to_clipboard_toast))
    }

}