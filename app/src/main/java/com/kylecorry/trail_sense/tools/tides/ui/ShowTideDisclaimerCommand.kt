package com.kylecorry.trail_sense.tools.tides.ui

import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils

class ShowTideDisclaimerCommand(private val fragment: Fragment, private val onAgree: () -> Unit) :
    Command {
    override fun execute() {
        val context = fragment.requireContext()

        val disclaimer = context.getString(R.string.disclaimer_estimated_tide)
        val tideInstructions = context.getString(R.string.tide_instructions)
        val tideTableLocation = context.getString(R.string.tide_user_guide_recommendation)

        val message = buildSpannedString {
            append(disclaimer)
            append("\n\n")
            bold {
                append(tideInstructions)
            }
            append("\n\n")
            append(tideTableLocation)
        }
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.tides),
            message,
            context.getString(R.string.pref_tide_disclaimer_seen),
            considerShownIfCancelled = true,
            cancelText = context.getString(R.string.tool_user_guide_title)
        ) { showGuide, _ ->
            if (showGuide) {
                UserGuideUtils.openGuide(
                    fragment,
                    R.raw.tides
                )
            } else {
                onAgree()
            }
        }
    }
}