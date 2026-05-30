package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getColorOnPrimary
import kotlin.math.max
import kotlin.math.min

class TileButton(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private var textView: TextView
    private var button: MaterialButton

    private var isOn = false
    private var tilePadding = resources.getDimensionPixelSize(R.dimen.gesture_zone)

    init {
        inflate(context, R.layout.view_tile_button, this)
        textView = findViewById(R.id.tile_text)
        button = findViewById(R.id.tile_btn)
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.TileButton, 0, 0)
        setImageResource(a.getResourceId(R.styleable.TileButton_tileIcon, R.drawable.flashlight))
        val padding = a.getDimensionPixelSize(R.styleable.TileButton_tilePadding, -1)
        val textSize = a.getDimension(R.styleable.TileButton_tileTextSize, -1f)
        textView.text = a.getString(R.styleable.TileButton_tileText)
        a.recycle()
        if (padding != -1) {
            setTilePadding(padding)
        }
        if (textSize != -1f) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        }
        setState(false)
    }

    fun setImageResource(@DrawableRes resId: Int) {
        button.setIconResource(resId)
        setState(isOn)
    }

    fun setState(on: Boolean) {
        isOn = on
        button.isChecked = on
        textView.setTextColor(
            if (on) {
                Resources.getColorOnPrimary(context)
            } else {
                Resources.androidTextColorSecondary(context)
            }
        )
    }

    fun setText(text: String?) {
        textView.text = text
    }

    fun setTilePadding(padding: Int) {
        tilePadding = padding
        updateIconSize()
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        button.setOnClickListener(listener)
    }

    override fun setOnLongClickListener(listener: OnLongClickListener?) {
        button.setOnLongClickListener(listener)
    }

    override fun setOnTouchListener(listener: OnTouchListener?) {
        button.setOnTouchListener(listener)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateIconSize()
    }

    private fun updateIconSize() {
        if (width == 0 || height == 0) {
            return
        }
        button.iconSize = max(0, min(width, height) - tilePadding * 2)
    }

}
