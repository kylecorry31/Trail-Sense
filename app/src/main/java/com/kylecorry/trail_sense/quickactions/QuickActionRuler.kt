package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.isMetric
import com.kylecorry.trail_sense.tools.ruler.ui.RulerView

class QuickActionRuler(
    btn: ImageButton,
    fragment: Fragment,
    private val ruler: RulerView
) : QuickActionButton(btn, fragment) {
    private val prefs by lazy { UserPreferences(context) }

    override fun onCreate() {
        super.onCreate()
        button.setImageResource(R.drawable.ruler)
        ruler.metric = prefs.baseDistanceUnits.isMetric()
        ruler.setOnTouchListener {
            CustomUiUtils.setButtonState(button, false)
            ruler.isVisible = false
        }
        button.setOnClickListener {
            if (ruler.isVisible) {
                CustomUiUtils.setButtonState(button, false)
                ruler.isVisible = false
            } else {
                CustomUiUtils.setButtonState(button, true)
                ruler.isVisible = true
            }
        }
    }
}