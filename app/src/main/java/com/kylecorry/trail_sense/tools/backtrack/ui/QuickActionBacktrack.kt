package com.kylecorry.trail_sense.tools.backtrack.ui

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.andromeda.core.time.Timer
import java.time.Duration

class QuickActionBacktrack(btn: FloatingActionButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private val prefs by lazy { UserPreferences(context) }

    private val intervalometer = Timer {
        update()
    }

    private fun update() {
        CustomUiUtils.setButtonState(
            button,
            prefs.backtrackEnabled && !(prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack)
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