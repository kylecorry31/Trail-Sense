package com.kylecorry.trail_sense.tools.field_guide.quickactions

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionRecordSighting(btn: QuickActionButtonView, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.field_guide)
    }

    override fun onClick() {
        super.onClick()
        FieldGuideUtils.showPageList(fragment) {
            fragment.toast(it.name)
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.FIELD_GUIDE)
        return true
    }
}
