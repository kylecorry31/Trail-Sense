package com.kylecorry.trail_sense.quickactions

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.databinding.ActivityNavigatorBinding
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.navigation.ui.NavigatorFragment
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.QuickActionType
import com.kylecorry.trail_sense.shared.views.QuickActionNone

class NavigationQuickActionBinder(
    private val fragment: NavigatorFragment,
    private val binding: ActivityNavigatorBinding,
    private val prefs: NavigationPreferences
) : IQuickActionBinder {

    override fun bind() {
        getQuickActionButton(
            prefs.leftQuickAction,
            binding.navigationLeftQuickAction
        ).bind(fragment)

        getQuickActionButton(
            prefs.rightQuickAction,
            binding.navigationRightQuickAction
        ).bind(fragment)
    }

    private fun getQuickActionButton(
        type: QuickActionType,
        button: FloatingActionButton
    ): QuickActionButton {
        return when (type) {
            QuickActionType.None -> QuickActionNone(button, fragment)
            QuickActionType.Backtrack -> QuickActionBacktrack(button, fragment)
            QuickActionType.Flashlight -> QuickActionFlashlight(button, fragment)
            QuickActionType.Ruler -> QuickActionRuler(button, fragment, binding.ruler)
            QuickActionType.Maps -> QuickActionOfflineMaps(button, fragment)
            QuickActionType.Whistle -> QuickActionWhistle(button, fragment)
            QuickActionType.LowPowerMode -> LowPowerQuickAction(button, fragment)
            else -> QuickActionNone(button, fragment)
        }
    }
}