package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.ruler.ui.Ruler

class QuickActionRuler(
    btn: ImageButton,
    fragment: Fragment,
    private val rulerView: ConstraintLayout
) : QuickActionButton(btn, fragment) {
    private lateinit var ruler: Ruler
    private val prefs by lazy { UserPreferences(context) }

    override fun onCreate() {
        super.onCreate()
        button.setImageResource(R.drawable.ruler)
        ruler = Ruler(
            rulerView,
            if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) DistanceUnits.Centimeters else DistanceUnits.Inches
        )
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
}