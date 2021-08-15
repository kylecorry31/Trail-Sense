package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R

class TileButton(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private var textView: TextView
    private var icon: ImageView

    private var isOn = false

    init {
        inflate(context, R.layout.view_tile_button, this)
        textView = findViewById(R.id.tile_text)
        icon = findViewById(R.id.tile_btn)
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.TileButton, 0, 0)
        setImageResource(a.getResourceId(R.styleable.TileButton_tileIcon, R.drawable.flashlight))
        val padding = a.getDimensionPixelSize(R.styleable.TileButton_tilePadding, -1)
        val textSize = a.getDimension(R.styleable.TileButton_tileTextSize, -1f)
        textView.text = a.getString(R.styleable.TileButton_tileText)
        a.recycle()
        if (padding != -1){
            icon.setPadding(padding)
        }
        if (textSize != -1f){
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        }
        setState(false)
    }

    fun setImageResource(@DrawableRes resId: Int) {
        icon.setImageResource(resId)
        setState(isOn)
    }

    fun setState(on: Boolean) {
        isOn = on
        if (isOn) {
            icon.backgroundTintList =
                ColorStateList.valueOf(Resources.color(icon.context, R.color.colorPrimary))
            textView.setTextColor(Resources.color(icon.context, R.color.colorSecondary))
            icon.imageTintList =
                ColorStateList.valueOf(Resources.color(icon.context, R.color.colorSecondary))
        } else {
            textView.setTextColor(Resources.androidTextColorSecondary(icon.context))
            icon.imageTintList =
                ColorStateList.valueOf(Resources.androidTextColorSecondary(icon.context))
            icon.backgroundTintList =
                ColorStateList.valueOf(Resources.androidBackgroundColorSecondary(icon.context))
        }
    }

    fun setText(text: String?) {
        textView.text = text
    }


}