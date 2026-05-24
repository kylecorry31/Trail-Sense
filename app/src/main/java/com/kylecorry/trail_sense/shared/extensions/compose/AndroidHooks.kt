package com.kylecorry.trail_sense.shared.extensions.compose

import android.content.Context
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator

@Composable
fun useAndroidContext(): Context {
    return LocalContext.current
}

@Composable
inline fun <reified T : Any> useService(): T {
    return useMemo { getAppService<T>() }
}

@Composable
fun Fragment.useRootView(): View {
    return requireView()
}

@Composable
fun Fragment.useNavController(): NavController {
    val rootView = useRootView()
    return useMemo(rootView) { findNavController() }
}

@Composable
fun Fragment.useActivity(): FragmentActivity {
    return requireActivity()
}

@Composable
fun Fragment.useBackPressedCallback(
    vararg values: Any?,
    callback: OnBackPressedCallback.() -> Boolean
) {
    val navController = useNavController()
    useEffectWithCleanup(*values) {
        val listener = onBackPressed {
            val consumed = callback()
            if (!consumed) {
                remove()
                navController.popBackStack()
            }
        }

        return@useEffectWithCleanup {
            listener.remove()
        }
    }
}

@Composable
fun Fragment.useUnsavedChangesPrompt(hasChanges: Boolean) {
    val activity = useActivity()
    useBackPressedCallback(hasChanges, activity) {
        if (hasChanges) {
            Alerts.dialog(
                activity,
                getString(R.string.unsaved_changes),
                getString(R.string.unsaved_changes_message),
                okText = getString(R.string.dialog_leave)
            ) { cancelled ->
                if (!cancelled) {
                    remove()
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            }
            true
        } else {
            false
        }
    }
}

@Composable
fun Fragment.useShowDisclaimer(
    title: String,
    message: CharSequence,
    shownKey: String,
    okText: String = getString(android.R.string.ok),
    cancelText: String? = getString(android.R.string.cancel),
    considerShownIfCancelled: Boolean = true,
    shownValue: Boolean = true,
    onClose: (cancelled: Boolean, agreed: Boolean) -> Unit = { _, _ -> }
) {
    val context = useAndroidContext()
    useEffect {
        CustomUiUtils.disclaimer(
            context,
            title,
            message,
            shownKey,
            okText,
            cancelText,
            considerShownIfCancelled,
            shownValue,
            onClose
        )
    }
}

@Composable
fun useLoadingIndicator(isLoading: Boolean, indicator: ILoadingIndicator) {
    useEffect(indicator, isLoading) {
        if (isLoading) {
            indicator.show()
        } else {
            indicator.hide()
        }
    }
}
