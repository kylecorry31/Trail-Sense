package com.kylecorry.trail_sense.shared.quickactions

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.widget.ImageButton
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.kylecorry.trail_sense.shared.CustomUiUtils

class ImageButtonQuickActionView(val button: ImageButton) : QuickActionButtonView {

    override var isVisible: Boolean
        get() = button.isVisible
        set(value) {
            button.isVisible = value
        }

    override var contentDescription: CharSequence?
        get() = button.contentDescription
        set(value) {
            button.contentDescription = value
        }

    override fun setIcon(@DrawableRes icon: Int) {
        button.setImageResource(icon)
    }

    override fun setState(isOn: Boolean) {
        CustomUiUtils.setButtonState(button, isOn)
    }

    override fun setOnClickListener(listener: (() -> Unit)?) {
        button.setOnClickListener { listener?.invoke() }
    }

    override fun setOnLongClickListener(listener: (() -> Boolean)?) {
        button.setOnLongClickListener { listener?.invoke() ?: false }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setOnTouchListener(listener: ((event: MotionEvent) -> Boolean)?) {
        button.setOnTouchListener { _, event -> listener?.invoke(event) ?: false }
    }
}
