package com.kylecorry.trail_sense.tools.whistle.ui

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trailsensecore.infrastructure.audio.Whistle

class QuickActionWhistle(btn: FloatingActionButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private lateinit var whistle: ISoundPlayer

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        whistle = Whistle()
        button.setImageResource(R.drawable.ic_tool_whistle)
        CustomUiUtils.setButtonState(button, false)
        button.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                whistle.on()
                CustomUiUtils.setButtonState(button, true)
            } else if (event.action == MotionEvent.ACTION_UP) {
                whistle.off()
                CustomUiUtils.setButtonState(button, false)
            }
            true
        }
    }

    override fun onResume() {
        CustomUiUtils.setButtonState(button, false)
    }

    override fun onPause() {
        whistle.off()
    }

    override fun onDestroy() {
        whistle.release()
    }

}