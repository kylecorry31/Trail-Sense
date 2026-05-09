package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import android.text.util.Linkify
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils

class ShowMapsDisclaimerCommand(private val fragment: Fragment) : Command {
    override fun execute() {
        val context = fragment.requireContext()
        val message = context.getString(R.string.photo_map_disclaimer).toSpannable()
        LinkifyCompat.addLinks(message, Linkify.WEB_URLS)
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.photo_maps),
            message,
            context.getString(R.string.photo_map_disclaimer_shown),
            okText = context.getString(android.R.string.ok),
            considerShownIfCancelled = true,
            cancelText = context.getString(R.string.tool_user_guide_title),
            shownValue = true
        ) { showUserGuide, _ ->
            if (showUserGuide) {
                UserGuideUtils.showGuide(fragment, R.raw.guide_tool_offline_maps)
            }
        }
    }
}
