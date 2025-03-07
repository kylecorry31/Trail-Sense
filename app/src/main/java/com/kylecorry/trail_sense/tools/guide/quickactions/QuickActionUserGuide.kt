package com.kylecorry.trail_sense.tools.guide.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionUserGuide(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {
    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.user_guide)
    }

    override fun onClick() {
        super.onClick()
        val activeTool = Tools.getTools(context).firstOrNull {
            it.isOpen(fragment.findNavController().currentDestination?.id ?: 0)
        }
        if (activeTool?.guideId != null) {
            UserGuideUtils.showGuide(fragment, activeTool.guideId)
        } else {
            UserGuideUtils.showGuide(fragment, R.raw.guide_tool_tools)
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.USER_GUIDE)
        return true
    }
}