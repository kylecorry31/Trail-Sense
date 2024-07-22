package com.kylecorry.trail_sense.shared.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.views.QuickActionNone
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionFactory {

    fun create(
        id: Int,
        button: ImageButton,
        fragment: Fragment
    ): QuickActionButton {
        val quickActions = Tools.getQuickActions(fragment.requireContext())
        button.contentDescription = "quick_action_$id"
        return quickActions.firstOrNull { it.id == id }?.create?.invoke(button, fragment)
            ?: QuickActionNone(button, fragment)
    }

}