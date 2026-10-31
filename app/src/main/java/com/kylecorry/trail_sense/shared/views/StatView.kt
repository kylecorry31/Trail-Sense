package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.kylecorry.trail_sense.R
import kotlin.math.roundToInt

class StatView(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private val valueView: TextView
    private val labelView: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val horizontalPadding = (context.resources.displayMetrics.density * PADDING_DP).roundToInt()
        setPaddingRelative(horizontalPadding, 0, horizontalPadding, 0)
        inflate(context, R.layout.view_stat, this)
        valueView = findViewById(R.id.stat_value)
        labelView = findViewById(R.id.stat_label)
    }

    fun show(
        valueText: String?,
        labelText: String,
        @DrawableRes valueIcon: Int? = null,
        @DrawableRes labelIcon: Int? = null,
        description: String? = null
    ) {
        isVisible = valueText != null
        contentDescription = description
        valueView.text = valueText
        labelView.text = labelText
        valueView.setStartIcon(valueIcon)
        labelView.setStartIcon(labelIcon)
    }

    fun hide() {
        isVisible = false
    }

    companion object {
        private const val PADDING_DP = 6f
    }
}

private const val ICON_SIZE_RATIO = 0.8f

private fun TextView.setStartIcon(@DrawableRes icon: Int?) {
    val drawable = icon?.let { ContextCompat.getDrawable(context, it) }
    val size = (textSize * ICON_SIZE_RATIO).roundToInt()
    drawable?.setBounds(0, 0, size, size)
    setCompoundDrawablesRelative(drawable, null, null, null)
    compoundDrawablePadding = size / 2
    TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(currentTextColor))
}
