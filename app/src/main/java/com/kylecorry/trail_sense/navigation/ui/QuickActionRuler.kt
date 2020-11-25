package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.ViewMeasurementUtils
import kotlin.math.roundToInt

class QuickActionRuler(btn: FloatingActionButton, fragment: Fragment, private val rulerView: ConstraintLayout): QuickActionButton(btn, fragment) {
    private lateinit var ruler: Ruler

    override fun onCreate() {
//        val layout = ConstraintLayout(context)
//        layout.id = View.generateViewId()
//        layout.layoutParams = ConstraintLayout.LayoutParams((60 * ViewMeasurementUtils.density(context)).roundToInt(), ConstraintLayout.LayoutParams.MATCH_PARENT)
//        val view = fragment.view
//        if (view is ConstraintLayout){
//            view.addView(layout)
//            val constraints = ConstraintSet()
//            constraints.clone(view)
//            constraints.connect(layout.id, ConstraintSet.TOP, view.id, ConstraintSet.TOP)
//            constraints.connect(layout.id, ConstraintSet.BOTTOM, view.id, ConstraintSet.BOTTOM)
//            constraints.connect(layout.id, ConstraintSet.START, view.id, ConstraintSet.START)
//            constraints.applyTo(view)
//        }
        ruler = Ruler(rulerView)
        button.setOnClickListener {
            if (ruler.visible) {
                UiUtils.setButtonState(
                    button,
                    false,
                    UiUtils.color(context, R.color.colorPrimary),
                    UiUtils.color(context, R.color.colorSecondary)
                )
                ruler.hide()
            } else {
                UiUtils.setButtonState(
                    button,
                    true,
                    UiUtils.color(context, R.color.colorPrimary),
                    UiUtils.color(context, R.color.colorSecondary)
                )
                ruler.show()
            }
        }


    }

    override fun onResume() {
        // Nothing needed here
    }

    override fun onPause() {
        // Nothing needed here
    }

    override fun onDestroy() {
        // Nothing needed here
    }
}