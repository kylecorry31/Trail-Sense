package com.kylecorry.trail_sense.quickactions

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import java.time.Duration

class QuickActionBacktrack(btn: FloatingActionButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {
    
    private val timer = Timer {
        update()
    }

    private fun update() {
        CustomUiUtils.setButtonState(
            button,
            BacktrackScheduler.isOn(context)
        )
    }

    override fun onCreate() {
        super.onCreate()
        button.setImageResource(R.drawable.ic_tool_backtrack)
        button.setOnClickListener {
            fragment.findNavController()
                .navigate(R.id.action_navigatorFragment_to_fragmentBacktrack)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!timer.isRunning()) {
            timer.interval(Duration.ofSeconds(1))
        }
    }

    override fun onPause() {
        super.onPause()
        timer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        onPause()
    }

}