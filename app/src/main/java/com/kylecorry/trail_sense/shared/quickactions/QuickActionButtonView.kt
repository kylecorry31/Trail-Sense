package com.kylecorry.trail_sense.shared.quickactions

import android.view.MotionEvent
import androidx.annotation.DrawableRes

interface QuickActionButtonView {
    var isVisible: Boolean
    var contentDescription: CharSequence?

    fun setIcon(@DrawableRes icon: Int)
    fun setState(isOn: Boolean)
    fun setOnClickListener(listener: (() -> Unit)?)
    fun setOnLongClickListener(listener: (() -> Boolean)?)
    fun setOnTouchListener(listener: ((event: MotionEvent) -> Boolean)?)
}
