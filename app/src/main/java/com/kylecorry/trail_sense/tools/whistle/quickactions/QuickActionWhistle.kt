package com.kylecorry.trail_sense.tools.whistle.quickactions

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.tools.whistle.infrastructure.Whistle

class QuickActionWhistle(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private var whistle: ISoundPlayer? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        fragment.inBackground {
            onDefault {
                try {
                    whistle = Whistle()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // The whistle couldn't be instantiated
                    onMain {
                        button.isVisible = false
                    }
                }
            }
        }

        button.setImageResource(R.drawable.ic_tool_whistle)
        CustomUiUtils.setButtonState(button, false)
        button.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                whistle?.on()
                CustomUiUtils.setButtonState(button, true)
            } else if (event.action == MotionEvent.ACTION_UP) {
                whistle?.off()
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
        whistle?.off()
    }

    override fun onDestroy() {
        super.onDestroy()
        whistle?.release()
    }

}