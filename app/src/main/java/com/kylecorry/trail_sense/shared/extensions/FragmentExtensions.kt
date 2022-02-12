package com.kylecorry.trail_sense.shared.extensions

import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R

fun Fragment.onBackPressed(
    enabled: Boolean = true,
    onBackPressed: OnBackPressedCallback.() -> Unit
): OnBackPressedCallback {
    return requireActivity().onBackPressedDispatcher.addCallback(this, enabled, onBackPressed)
}

fun Fragment.promptIfUnsavedChanges(
    hasChanges: () -> Boolean
): OnBackPressedCallback {
    val activity = requireActivity()
    return onBackPressed {
        if (hasChanges()) {
            Alerts.dialog(
                activity,
                getString(R.string.unsaved_changes),
                getString(R.string.unsaved_changes_message),
                okText = getString(R.string.dialog_leave)
            ) { cancelled ->
                if (!cancelled) {
                    remove()
                    activity.onBackPressed()
                }
            }
        } else {
            remove()
            activity.onBackPressed()
        }
    }
}