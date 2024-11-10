package com.kylecorry.trail_sense.tools.astronomy.summaries

import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.tools.ui.summaries.SmallSimpleToolSummaryView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MoonToolSummaryView(root: FrameLayout, fragment: Fragment) : SmallSimpleToolSummaryView(
    root,
    fragment
) {

    private val astronomyService = AstronomyService()
    private val location = LocationSubsystem.getInstance(context)
    private val formatter = FormatService.getInstance(context)

    override fun onCreate() {
        super.onCreate()
        fragment.inBackground {
            populateMoonDetails()
        }
    }

    // TODO: Extract this method
    private suspend fun populateMoonDetails() {
        var phase: MoonPhase
        var tilt: Float
        withContext(Dispatchers.Default) {
            phase = astronomyService.getCurrentMoonPhase()
            tilt = astronomyService.getMoonTilt(location.location)
        }

        val image = MoonPhaseImageMapper().getPhaseImage(phase.phase)

        onMain {
            binding.summaryIcon.setImageResource(image)
            binding.summaryIcon.rotation = tilt
            binding.summaryTitle.text = context.getString(R.string.moon)
            binding.summarySubtitle.text = formatter.formatMoonPhase(phase.phase)
        }
    }
}