package com.kylecorry.trail_sense.tools.tools.ui.summaries

import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.databinding.SummarySmallSimpleBinding

open class SmallSimpleToolSummaryView(root: FrameLayout, fragment: Fragment) :
    ToolSummaryView(root, fragment) {

    private var _binding: SummarySmallSimpleBinding? = null

    protected val binding: SummarySmallSimpleBinding
        get() {
            return _binding!!
        }

    override fun onCreate() {
        super.onCreate()
        _binding = SummarySmallSimpleBinding.inflate(fragment.layoutInflater, root, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}