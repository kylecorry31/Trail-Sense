package com.kylecorry.trail_sense.tools.paths.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.quickactions.ToolServiceQuickAction
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.ui.commands.ToggleBacktrackCommand

class QuickActionBacktrack(btn: ImageButton, fragment: Fragment) :
    ToolServiceQuickAction(
        btn,
        fragment,
        PathsToolRegistration.SERVICE_BACKTRACK,
        PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED,
        hideWhenUnavailable = false
    ) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_tool_backtrack)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().navigateWithAnimation(R.id.fragmentBacktrack)
        return true
    }

    override fun onClick() {
        super.onClick()
        val command = ToggleBacktrackCommand(fragment)
        command.execute()
    }
}