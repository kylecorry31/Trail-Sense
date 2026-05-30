package com.kylecorry.trail_sense.shared.quickactions

import android.view.MotionEvent
import androidx.annotation.DrawableRes

/**
 * An abstraction over the widget backing a quick action button. This lets quick actions work with
 * a [com.google.android.material.button.MaterialButton] where possible and fall back to an
 * [android.widget.ImageButton] (for example, the buttons provided by the toolbar).
 */
interface QuickActionButtonView {
    var isVisible: Boolean
    var contentDescription: CharSequence?

    fun setIcon(@DrawableRes icon: Int)
    fun setState(isOn: Boolean)
    fun setOnClickListener(listener: (() -> Unit)?)
    fun setOnLongClickListener(listener: (() -> Boolean)?)
    fun setOnTouchListener(listener: ((event: MotionEvent) -> Boolean)?)
}
