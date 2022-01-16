package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils

class ToolTitleView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    val leftQuickAction: FloatingActionButton
    val rightQuickAction: FloatingActionButton
    val title: TextView
    val subtitle: TextView


    init {
        inflate(context, R.layout.view_tool_title, this)
        leftQuickAction = findViewById(R.id.left_quick_action)
        rightQuickAction = findViewById(R.id.right_quick_action)
        title = findViewById(R.id.title)
        subtitle = findViewById(R.id.subtitle)

        // Update attributes
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.ToolTitleView, 0, 0)
        title.text = a.getString(R.styleable.ToolTitleView_title) ?: ""
        subtitle.text = a.getString(R.styleable.ToolTitleView_subtitle) ?: ""

        val leftIcon = a.getResourceId(R.styleable.ToolTitleView_leftQuickActionIcon, -1)
        val rightIcon = a.getResourceId(R.styleable.ToolTitleView_rightQuickActionIcon, -1)

        if (leftIcon != -1) {
            leftQuickAction.isVisible = true
            leftQuickAction.setImageResource(leftIcon)
        }

        if (rightIcon != -1) {
            rightQuickAction.isVisible = true
            rightQuickAction.setImageResource(rightIcon)
        }

        CustomUiUtils.setButtonState(leftQuickAction, false)
        CustomUiUtils.setButtonState(rightQuickAction, false)
    }

}