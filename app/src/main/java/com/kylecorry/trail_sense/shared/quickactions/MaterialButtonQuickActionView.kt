package com.kylecorry.trail_sense.shared.quickactions

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton

class MaterialButtonQuickActionView(val button: MaterialButton) : QuickActionButtonView {

    init {
        button.isCheckable = true
        button.isToggleCheckedStateOnClick = false
    }

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
        button.setIconResource(icon)
    }

    override fun setState(isOn: Boolean) {
        button.isChecked = isOn
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
