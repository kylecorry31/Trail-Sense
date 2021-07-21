package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.setPadding
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

class ColorPickerView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val flex: FlexboxLayout
    private var changeListener: ((color: AppColor?) -> Unit)? = null

    var color: AppColor? = null
        set(value) {
            if (field != null) {
                (flex.getChildAt(field?.ordinal ?: 0) as ColorButton).isButtonSelected = false
            }
            if (value != null) {
                (flex.getChildAt(value.ordinal) as ColorButton).isButtonSelected = true
            }
            field = value
        }


    init {
        inflate(context, R.layout.view_color_picker, this)
        flex = findViewById(R.id.color_picker_flex)

        for (color in AppColor.values()) {
            val colorView = ColorButton(context, null)
            colorView.setButtonColor(color.color)
            colorView.setPadding(UiUtils.dp(context, 16f).toInt())
            colorView.setOnClickListener {
                this.color = color
                changeListener?.invoke(color)
            }
            flex.addView(colorView)
        }

    }

    fun setOnColorChangeListener(listener: ((color: AppColor?) -> Unit)?) {
        changeListener = listener
    }
}