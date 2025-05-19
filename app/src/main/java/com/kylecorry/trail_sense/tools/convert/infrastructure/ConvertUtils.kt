package com.kylecorry.trail_sense.tools.convert.infrastructure

import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.trail_sense.tools.convert.ui.ConvertBottomSheetFragment

object ConvertUtils {

    fun showConvert(fragment: Fragment): BottomSheetDialogFragment? {
        val sheet = ConvertBottomSheetFragment()
        sheet.show(fragment)
        return sheet
    }
}
