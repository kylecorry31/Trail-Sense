package com.kylecorry.trail_sense.navigation.beacons.infrastructure.share

import android.content.Context
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon

class BeaconCopy(private val context: Context) : IBeaconSender {

    override fun send(beacon: Beacon) {
        val formatService = FormatService.getInstance(context)
        val text = "${beacon.name}: ${formatService.formatLocation(beacon.coordinate)}"
        Clipboard.copy(context, text, context.getString(R.string.copied_to_clipboard_toast))
    }

}