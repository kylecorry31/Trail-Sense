package com.kylecorry.trail_sense.tools.backtrack.ui

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration

class QuickActionBacktrack(btn: FloatingActionButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private val prefs by lazy { UserPreferences(context) }

    private val intervalometer = Intervalometer {
        update()
    }

    private fun update() {
        UiUtils.setButtonState(
            button,
            prefs.backtrackEnabled,
            UiUtils.color(context, R.color.colorPrimary),
            UiUtils.color(context, R.color.colorSecondary)
        )
    }

    override fun onCreate() {
        button.setImageResource(R.drawable.ic_tool_backtrack)
        button.setOnClickListener {
            fragment.findNavController()
                .navigate(R.id.action_navigatorFragment_to_fragmentBacktrack)
        }
    }

    override fun onResume() {
        if (!intervalometer.isRunning()) {
            intervalometer.interval(Duration.ofSeconds(1))
        }
    }

    override fun onPause() {
        intervalometer.stop()
    }

    override fun onDestroy() {
        onPause()
    }

}