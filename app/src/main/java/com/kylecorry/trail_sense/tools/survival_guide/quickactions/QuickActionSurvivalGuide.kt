package com.kylecorry.trail_sense.tools.survival_guide.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionSurvivalGuide(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {
    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.survival_guide)
    }

    override fun onClick() {
        super.onClick()
        SurvivalUtils.showSurvival(fragment)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.SURVIVAL_GUIDE)
        return true
    }
}
