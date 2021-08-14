package com.kylecorry.trail_sense.shared

import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.andromeda.core.system.IntentUtils

object AppUtils {

    fun placeBeacon(context: Context, coordinate: MyNamedCoordinate) {
        val intent =
            IntentUtils.localIntent(context, "com.kylecorry.trail_sense.PLACE_BEACON").apply {
                data = if (coordinate.name == null) {
                    Uri.parse("geo:${coordinate.coordinate.latitude},${coordinate.coordinate.longitude}")
                } else {
                    Uri.parse("geo:0,0?q=${coordinate.coordinate.latitude},${coordinate.coordinate.longitude}(${coordinate.name})")
                }
            }
        ContextCompat.startActivity(context, intent, null)
    }

}