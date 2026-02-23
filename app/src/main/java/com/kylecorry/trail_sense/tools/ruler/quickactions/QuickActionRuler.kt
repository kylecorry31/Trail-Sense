package com.kylecorry.trail_sense.tools.ruler.quickactions

import android.view.ViewGroup
import android.widget.ImageButton
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.ruler.ui.RulerView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

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
        ruler.setBackgroundColor(
            Resources.getAndroidColorAttr(
                context,
                android.R.attr.colorBackgroundFloating
            )
        )

        ruler.x = 0f
        ruler.y = 0f
        val layoutParams = CoordinatorLayout.LayoutParams(
            Resources.dp(context, 80f).toInt(),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val bottomNavigation = fragment.requireActivity().findViewById<ViewGroup>(R.id.bottom_navigation)
        if (bottomNavigation.height > 0) {
            layoutParams.bottomMargin = bottomNavigation.height
        } else {
            bottomNavigation.doOnLayout {
                val params = ruler.layoutParams as? CoordinatorLayout.LayoutParams ?: return@doOnLayout
                params.bottomMargin = it.height
                ruler.layoutParams = params
            }
        }
        ruler.layoutParams = layoutParams

        val root = fragment.requireActivity().findViewById(R.id.coordinator) as? CoordinatorLayout
        root?.addView(ruler)

        setIcon(R.drawable.ruler)
        ruler.metric = prefs.baseDistanceUnits.isMetric
        ruler.setOnTouchListener {
            setState(false)
            ruler.isVisible = false
        }
    }

    override fun onClick() {
        super.onClick()
        if (ruler?.isVisible == true) {
            setState(false)
            ruler?.isVisible = false
        } else {
            setState(true)
            ruler?.isVisible = true
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.RULER)
        return true
    }

    override fun onPause() {
        super.onPause()
        if (ruler?.isVisible == true) {
            setState(false)
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