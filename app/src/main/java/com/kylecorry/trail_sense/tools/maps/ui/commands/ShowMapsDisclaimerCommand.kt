package com.kylecorry.trail_sense.tools.maps.ui.commands

import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.commands.Command

class ShowMapsDisclaimerCommand(private val fragment: Fragment) : Command {
    override fun execute() {
        val context = fragment.requireContext()
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.experimental),
            "Photo Maps is an experimental feature, please only use this to test it out at this point. Feel free to share your feedback on this feature and note that there is still a lot to be done before this will be non-experimental.",
            "tool_maps_experimental_disclaimer_shown",
            okText = context.getString(android.R.string.ok),
            considerShownIfCancelled = true,
            cancelText = null,
            shownValue = true
        )
    }
}