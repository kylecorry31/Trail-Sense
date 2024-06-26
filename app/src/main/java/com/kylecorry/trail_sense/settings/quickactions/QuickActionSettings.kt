package com.kylecorry.trail_sense.settings.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionSettings(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_settings)
    }

    override fun onClick() {
        super.onClick()
        val activeTool = Tools.getTools(context).firstOrNull {
            it.isOpen(fragment.findNavController().currentDestination?.id ?: 0)
        }
        if (activeTool?.settingsNavAction != null) {
            fragment.findNavController().navigateWithAnimation(activeTool.settingsNavAction)
        } else {
            fragment.findNavController().openTool(Tools.SETTINGS)
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.SETTINGS)
        return true
    }
}