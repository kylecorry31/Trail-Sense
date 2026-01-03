package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Intents

object AppUtils {

    fun placeBeacon(context: Context, geo: GeoUri) {
        val intent =
            Intents.localIntent(context, "com.kylecorry.trail_sense.PLACE_BEACON").apply {
                data = geo.uri
            }
        context.startActivity(intent, null)
    }

}