package com.kylecorry.trail_sense.tools.photo_maps.ui.commands

import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils

class ShowMapsDisclaimerCommand(private val fragment: Fragment) : Command {
    override fun execute() {
        val context = fragment.requireContext()
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.photo_maps),
            context.getString(R.string.photo_map_disclaimer),
            context.getString(R.string.photo_map_disclaimer_shown),
            okText = context.getString(android.R.string.ok),
            considerShownIfCancelled = true,
            cancelText = context.getString(R.string.tool_user_guide_title),
            shownValue = true
        ) { showUserGuide, _ ->
            if (showUserGuide) {
                UserGuideUtils.showGuide(fragment, R.raw.guide_tool_photo_maps)
            }
        }
    }
}