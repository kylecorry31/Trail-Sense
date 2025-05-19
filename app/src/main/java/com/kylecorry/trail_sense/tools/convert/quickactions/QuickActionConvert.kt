package com.kylecorry.trail_sense.tools.convert.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.convert.infrastructure.ConvertUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionConvert(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {
    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_tool_distance_convert)
    }

    override fun onClick() {
        super.onClick()
        ConvertUtils.showConvert(fragment)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.CONVERT)
        return true
    }
}
