package com.kylecorry.trail_sense.tools.experimentation

import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.views.Views
import com.kylecorry.trail_sense.tools.tools.ui.ToolSummaryView

class ExperimentationToolSummaryView(root: FrameLayout, fragment: Fragment) : ToolSummaryView(
    root,
    fragment
) {

    override fun onCreate() {
        super.onCreate()
        val textView = Views.text(context, "This is a test")
        root.addView(textView)
    }

}