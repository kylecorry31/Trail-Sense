package com.kylecorry.trail_sense.tools.ui

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils

class ToolButton(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val textView: TextView

    private val iconSize = Resources.dp(context, 24f).toInt()
    private val iconPadding = Resources.dp(context, 12f).toInt()
    private val iconColor = Resources.androidTextColorPrimary(context)
    private val buttonHeight = Resources.dp(context, 64f).toInt()
    private val buttonPadding = Resources.dp(context, 16f).toInt()
    private val buttonBackgroundColor =
        Resources.getAndroidColorAttr(context, android.R.attr.colorBackgroundFloating)

    var text: CharSequence?
        get() = textView.text
        set(value) {
            textView.text = value
        }

    fun setIconResource(icon: Int) {
        textView.setCompoundDrawables(iconSize, left = icon)
        CustomUiUtils.setImageColor(textView, iconColor)
    }

    fun setOnClickListener(listener: (View) -> Unit) {
        textView.setOnClickListener(listener)
    }

    fun setOnLongClickListener(listener: (View) -> Boolean) {
        textView.setOnLongClickListener(listener)
    }

    init {
        textView = TextView(context)
        textView.compoundDrawablePadding = iconPadding
        textView.elevation = 2f
        textView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, buttonHeight)
        textView.gravity = Gravity.CENTER_VERTICAL
        textView.setPadding(buttonPadding, 0, buttonPadding, 0)

        textView.setBackgroundResource(R.drawable.rounded_rectangle)
        textView.backgroundTintList = ColorStateList.valueOf(buttonBackgroundColor)

        addView(textView)
    }

}