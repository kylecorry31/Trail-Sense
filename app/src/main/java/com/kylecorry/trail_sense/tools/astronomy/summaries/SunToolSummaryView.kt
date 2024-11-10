package com.kylecorry.trail_sense.tools.astronomy.summaries

import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.luna.timer.CoroutineTimer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyTransition
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.summaries.SmallSimpleToolSummaryView
import java.time.Duration
import java.time.LocalDateTime

class SunToolSummaryView(root: FrameLayout, fragment: Fragment) : SmallSimpleToolSummaryView(
    root,
    fragment
) {

    private val formatService = FormatService.getInstance(context)
    private val astronomy = AstronomySubsystem.getInstance(context)
    private val timer = CoroutineTimer {
        populateSunDetails()
    }

    override fun onResume() {
        super.onResume()
        timer.interval(1000 * 60)
    }

    override fun onPause() {
        super.onPause()
        timer.stop()
    }

    private suspend fun populateSunDetails() {
        val sun = onDefault { astronomy.sun }

        onMain {
            if (sun.nextRise != null && sun.nextTransition == AstronomyTransition.Rise) {
                val time =
                    formatService.formatTime(sun.nextRise.toLocalTime(), includeSeconds = false)
                val timeUntil = formatService.formatDuration(
                    Duration.between(
                        LocalDateTime.now(),
                        sun.nextRise
                    )
                )
                binding.summarySubtitle.text = "$time ($timeUntil)"
                binding.summaryTitle.text = context.getString(R.string.sunrise)
                binding.summaryIcon.setImageResource(R.drawable.ic_sunrise_notification)
            } else if (sun.nextSet != null && sun.nextTransition == AstronomyTransition.Set) {
                val time =
                    formatService.formatTime(sun.nextSet.toLocalTime(), includeSeconds = false)
                val timeUntil = formatService.formatDuration(
                    Duration.between(
                        LocalDateTime.now(),
                        sun.nextSet
                    )
                )
                binding.summarySubtitle.text = "$time ($timeUntil)"
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