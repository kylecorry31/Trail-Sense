package com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands

import android.content.Context
import android.text.util.Linkify.WEB_URLS
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.commands.Command

class ShowOfflineMapsDisclaimerCommand(private val context: Context) : Command {
    override fun execute() {
        val message = context.getString(R.string.offline_maps_disclaimer).toSpannable()
        LinkifyCompat.addLinks(message, WEB_URLS)

        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.offline_maps),
            message,
            context.getString(R.string.pref_offline_maps_disclaimer_shown),
            cancelText = null,
            considerShownIfCancelled = true,
            shownValue = true
        )
    }
}
