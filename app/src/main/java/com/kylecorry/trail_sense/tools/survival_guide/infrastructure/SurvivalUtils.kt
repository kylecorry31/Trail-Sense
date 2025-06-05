package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.trail_sense.tools.survival_guide.ui.SurvivalGuideBottomSheetFragment

object SurvivalUtils {

    fun showSurvival(fragment: Fragment): BottomSheetDialogFragment? {
        val sheet = SurvivalGuideBottomSheetFragment()
        sheet.show(fragment)
        return sheet
    }
}
