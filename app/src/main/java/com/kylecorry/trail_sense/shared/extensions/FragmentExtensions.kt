package com.kylecorry.trail_sense.shared.extensions

import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.shared.CustomUiUtils

fun Fragment.promptIfUnsavedChanges(
    hasChanges: () -> Boolean
): OnBackPressedCallback {
    val activity = requireActivity()
    return onBackPressed {
        val leave = {
            remove()
            activity.onBackPressedDispatcher.onBackPressed()
        }
        if (hasChanges()) {
            CustomUiUtils.confirmDiscardChanges(activity, leave)
        } else {
            leave()
        }
    }
}

fun Fragment.getMarkdown(@StringRes resId: Int, vararg formatArgs: Any?): CharSequence {
    val service = DependencyRegistry.get<MarkdownService>()
    return service.toMarkdown(getString(resId, *formatArgs))
}
