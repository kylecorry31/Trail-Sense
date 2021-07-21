package com.kylecorry.trail_sense.tools.ruler.ui

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.units.DistanceUnits

class QuickActionRuler(btn: FloatingActionButton, fragment: Fragment, private val rulerView: ConstraintLayout): QuickActionButton(btn, fragment) {
    private lateinit var ruler: Ruler
    private val prefs by lazy { UserPreferences(context) }

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
        button.setImageResource(R.drawable.ruler)
        ruler = Ruler(rulerView, if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) DistanceUnits.Centimeters else DistanceUnits.Inches)
        button.setOnClickListener {
            if (ruler.visible) {
                CustomUiUtils.setButtonState(button, false)
                ruler.hide()
            } else {
                CustomUiUtils.setButtonState(button, true)
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