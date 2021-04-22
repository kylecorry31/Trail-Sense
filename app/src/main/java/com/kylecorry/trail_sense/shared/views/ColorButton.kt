package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

class ColorButton(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val backgroundView: ImageView
    private val foregroundView: ImageView

    var isButtonSelected: Boolean
        get() = _isSelected
        set(value){
            _isSelected = value
            backgroundView.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    private var _isSelected = false

    init {
        inflate(context, R.layout.view_color_button, this)
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ColorButton,
            0,
            0
        )
        val color = a.getColor(R.styleable.ColorButton_buttonColor, Color.BLACK)
        a.recycle()

        backgroundView = findViewById(R.id.color_btn_background)
        foregroundView = findViewById(R.id.color_btn_foreground)

        foregroundView.imageTintList = ColorStateList.valueOf(color)
        isButtonSelected = false
    }

    fun setButtonColor(@ColorInt color: Int){
        foregroundView.imageTintList = ColorStateList.valueOf(color)
    }
}