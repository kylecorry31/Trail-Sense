package com.kylecorry.trail_sense.tools.astronomy.summaries

import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.tools.ui.summaries.SmallSimpleToolSummaryView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime

class SunToolSummaryView(root: FrameLayout, fragment: Fragment) : SmallSimpleToolSummaryView(
    root,
    fragment
) {

    private val astronomyService = AstronomyService()
    private val location = LocationSubsystem.getInstance(context)
    private val prefs = UserPreferences(context)
    private val sunTimesMode = prefs.astronomy.sunTimesMode
    private val formatService = FormatService.getInstance(context)

    override fun onCreate() {
        super.onCreate()
        fragment.inBackground {
            populateSunDetails()
        }
    }

    // TODO: Extract this method
    private suspend fun populateSunDetails() {
        val currentTime = LocalDateTime.now()

        var nextSunrise: LocalDateTime?
        var nextSunset: LocalDateTime?
        withContext(Dispatchers.Default) {
            nextSunrise = astronomyService.getNextSunrise(location.location, sunTimesMode)
            nextSunset = astronomyService.getNextSunset(location.location, sunTimesMode)
        }

        onMain {
            if (nextSunrise != null && (nextSunset == null || nextSunrise?.isBefore(nextSunset) == true)) {
                binding.summaryTitle.text = formatService.formatDuration(
                    Duration.between(
                        currentTime,
                        nextSunrise
                    )
                )
                binding.summarySubtitle.text = context.getString(R.string.until_sunrise)
                binding.summaryIcon.setImageResource(R.drawable.ic_sunrise_notification)
            } else if (nextSunset != null) {
                binding.summaryTitle.text = formatService.formatDuration(
                    Duration.between(
                        currentTime,
                        nextSunset
                    )
                )
                binding.summarySubtitle.text = context.getString(R.string.until_sunset)
                binding.summaryIcon.setImageResource(R.drawable.ic_sunset_notification)
            } else if (astronomyService.isSunUp(location.location)) {
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
}