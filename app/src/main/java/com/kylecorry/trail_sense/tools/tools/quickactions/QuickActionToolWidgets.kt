package com.kylecorry.trail_sense.tools.tools.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetViewBottomSheet

class QuickActionToolWidgets(button: ImageButton, fragment: Fragment) : QuickActionButton(
    button,
    fragment
) {

    private var sheet: ToolWidgetViewBottomSheet? = null

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_summary)
    }

    override fun onClick() {
        super.onClick()
        sheet?.dismiss()
        sheet = ToolWidgetViewBottomSheet()
        sheet?.show(fragment)
    }

    override fun onPause() {
        super.onPause()
        sheet?.dismissAllowingStateLoss()
        sheet = null
    }

}