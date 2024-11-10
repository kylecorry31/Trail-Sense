package com.kylecorry.trail_sense.tools.astronomy.summaries

import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.summaries.SmallSimpleToolSummaryView

class MoonToolSummaryView(root: FrameLayout, fragment: Fragment) : SmallSimpleToolSummaryView(
    root,
    fragment
) {

    private val astronomy = AstronomySubsystem.getInstance(context)
    private val formatter = FormatService.getInstance(context)

    override fun onCreate() {
        super.onCreate()
        fragment.inBackground {
            populateMoonDetails()
        }
    }

    private suspend fun populateMoonDetails() {
        val moon = onDefault { astronomy.moon }

        val image = MoonPhaseImageMapper().getPhaseImage(moon.phase)

        onMain {
            binding.summaryIcon.setImageResource(image)
            binding.summaryIcon.rotation = moon.tilt
            binding.summaryTitle.text = context.getString(R.string.moon)
            binding.summarySubtitle.text = formatter.formatMoonPhase(moon.phase)
        }
    }

    override fun onClick() {
        super.onClick()
        fragment.findNavController().openTool(Tools.ASTRONOMY)
    }
}