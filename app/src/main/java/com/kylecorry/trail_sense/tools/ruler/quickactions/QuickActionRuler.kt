package com.kylecorry.trail_sense.tools.ruler.quickactions

import android.view.ViewGroup
import android.widget.ImageButton
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.isMetric
import com.kylecorry.trail_sense.tools.ruler.ui.RulerView

class QuickActionRuler(
    btn: ImageButton,
    fragment: Fragment,
) : QuickActionButton(btn, fragment) {
    private val prefs by lazy { UserPreferences(context) }

    private var ruler: RulerView? = null

    override fun onCreate() {
        super.onCreate()

        if (ruler != null) {
            removeRuler()
        }

        val ruler = RulerView(context)
        this.ruler = ruler
        ruler.isVisible = false
        ruler.elevation = Resources.dp(context, 4f)
        ruler.setBackgroundColor(
            Resources.getAndroidColorAttr(
                context,
                android.R.attr.colorBackgroundFloating
            )
        )

        ruler.x = 0f
        ruler.y = 0f
        ruler.layoutParams = ViewGroup.LayoutParams(
            Resources.dp(context, 80f).toInt(),
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val root = fragment.requireActivity().findViewById(R.id.coordinator) as? CoordinatorLayout
        root?.addView(ruler)

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

    override fun onPause() {
        super.onPause()
        if (ruler?.isVisible == true) {
            CustomUiUtils.setButtonState(button, false)
            ruler?.isVisible = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeRuler()
    }

    private fun removeRuler() {
        ruler?.let {
            val root =
                fragment.requireActivity().findViewById(R.id.coordinator) as? CoordinatorLayout
            root?.removeView(it)
        }
        ruler = null
    }

}