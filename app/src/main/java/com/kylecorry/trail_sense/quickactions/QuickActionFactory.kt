package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.views.QuickActionNone
import com.kylecorry.trail_sense.tools.ui.Tools

class QuickActionFactory {

    fun create(
        id: Int,
        button: ImageButton,
        fragment: AndromedaFragment
    ): QuickActionButton {
        val quickActions = Tools.getQuickActions(fragment.requireContext())
        return quickActions.firstOrNull { it.id == id }?.create?.invoke(button, fragment)
            ?: QuickActionNone(button, fragment)
    }

}