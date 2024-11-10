package com.kylecorry.trail_sense.tools.astronomy.summaries

import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyTransition
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.summaries.SmallSimpleToolSummaryView

class SunToolSummaryView(root: FrameLayout, fragment: Fragment) : SmallSimpleToolSummaryView(
    root,
    fragment
) {

    private val formatService = FormatService.getInstance(context)
    private val astronomy = AstronomySubsystem.getInstance(context)

    override fun onCreate() {
        super.onCreate()
        fragment.inBackground {
            populateSunDetails()
        }
    }

    private suspend fun populateSunDetails() {
        val sun = onDefault { astronomy.sun }

        onMain {
            if (sun.nextRise != null && sun.nextTransition == AstronomyTransition.Rise) {
                binding.summarySubtitle.text =
                    formatService.formatTime(sun.nextRise.toLocalTime(), includeSeconds = false)
                binding.summaryTitle.text = context.getString(R.string.sunrise)
                binding.summaryIcon.setImageResource(R.drawable.ic_sunrise_notification)
            } else if (sun.nextSet != null && sun.nextTransition == AstronomyTransition.Set) {
                binding.summarySubtitle.text =
                    formatService.formatTime(sun.nextSet.toLocalTime(), includeSeconds = false)
                binding.summaryTitle.text = context.getString(R.string.sunset)
                binding.summaryIcon.setImageResource(R.drawable.ic_sunset_notification)
            } else if (sun.isUp) {
                binding.summaryTitle.text = context.getString(R.string.sun_up_no_set)
                binding.summarySubtitle.text = context.getString(R.string.sun_does_not_set)
                binding.summaryIcon.setImageResource(R.drawable.ic_sun)
            } else {
                binding.summaryTitle.text = context.getString(R.string.sun_down_no_set)
                binding.summarySubtitle.text = context.getString(R.string.sun_does_not_rise)
                binding.summaryIcon.setImageResource(R.drawable.ic_sun)
            }
        }
    }

    override fun onClick() {
        super.onClick()
        fragment.findNavController().openTool(Tools.ASTRONOMY)
    }
}