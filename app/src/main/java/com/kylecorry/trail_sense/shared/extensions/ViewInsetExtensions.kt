package com.kylecorry.trail_sense.shared.extensions

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kylecorry.trail_sense.main.MainActivity

/**
 * Calls [onInset] with the height of the status bar (or display cutout) that this screen needs to
 * stay clear of. This is 0 unless the screen is drawn under the status bar (see
 * Tool.statusBarExtendedNavigationIds), since otherwise the main activity already offsets it.
 */
fun View.onTopInset(onInset: (top: Int) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
        val insets =
            windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val isExtended = context.findMainActivity()?.extendsUnderStatusBar ?: true
        onInset(if (isExtended) insets.top else 0)
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}

private fun Context.findMainActivity(): MainActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is MainActivity) {
            return current
        }
        current = current.baseContext
    }
    return null
}
