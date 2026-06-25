package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.trail_sense.shared.andromeda_temp.dismissOnPause
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuidePageListBottomSheetFragment

object FieldGuideUtils {

    fun showPageList(
        fragment: Fragment,
        onPageSelected: ((FieldGuidePage) -> Unit)? = null
    ): BottomSheetDialogFragment {
        val sheet = FieldGuidePageListBottomSheetFragment()
        sheet.onPageSelected = {
            onPageSelected?.invoke(it)
            sheet.dismiss()
        }
        sheet.show(fragment)
        sheet.dismissOnPause(fragment)
        return sheet
    }
}
