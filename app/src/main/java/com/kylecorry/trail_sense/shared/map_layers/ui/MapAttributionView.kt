package com.kylecorry.trail_sense.shared.map_layers.ui

import android.text.TextUtils
import android.widget.TextView
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R

fun TextView.setupMapAttribution() {
    maxLines = 1
    ellipsize = TextUtils.TruncateAt.END
    linksClickable = false
    setOnClickListener {
        Alerts.dialog(
            context,
            context.getString(R.string.attribution),
            text,
            cancelText = null,
            allowLinks = true
        )
    }
}
