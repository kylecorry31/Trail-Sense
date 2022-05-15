package com.kylecorry.trail_sense.quickactions

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.tools.whistle.infrastructure.Whistle

class QuickActionWhistle(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private lateinit var whistle: ISoundPlayer

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
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
        super.onResume()
        CustomUiUtils.setButtonState(button, false)
    }

    override fun onPause() {
        super.onPause()
        whistle.off()
    }

    override fun onDestroy() {
        super.onDestroy()
        whistle.release()
    }

}