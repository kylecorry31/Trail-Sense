package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R

class StatusBadgeView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var background: LinearLayout
    var statusText: TextView
    var statusImage: ImageView

    @ColorInt
    private var backgroundTint: Int = Color.WHITE

    @ColorInt
    private var foregroundTint: Int = Color.BLACK

    init {
        context.let {
            inflate(it, R.layout.view_status_badge, this)
            background = findViewById(R.id.status_view)
            statusText = findViewById(R.id.status_text)
            statusImage = findViewById(R.id.status_image)
            val a = it.theme.obtainStyledAttributes(attrs, R.styleable.StatusBadgeView, 0, 0)
            backgroundTint = a.getColor(R.styleable.StatusBadgeView_backgroundTint, Resources.androidBackgroundColorSecondary(it))
            foregroundTint = a.getColor(R.styleable.StatusBadgeView_foregroundTint, Resources.androidTextColorSecondary(it))
            setImageResource(a.getResourceId(R.styleable.StatusBadgeView_icon, R.drawable.satellite))
            a.recycle()
        }
    }

    fun setImageResource(@DrawableRes resId: Int) {
        statusImage.setImageResource(resId)
        statusImage.imageTintList = ColorStateList.valueOf(foregroundTint)
    }

    fun setForegroundTint(@ColorInt color: Int){
        statusImage.imageTintList = ColorStateList.valueOf(color)
        statusText.setTextColor(color)
        foregroundTint = color
    }

    fun setBackgroundTint(@ColorInt color: Int){
        background.backgroundTintList = ColorStateList.valueOf(color)
        backgroundTint = color
    }

    fun setStatusText(text: String?){
        if (text == null){
            statusText.text = ""
            statusText.visibility = View.GONE
        } else {
            statusText.text = text
            statusText.visibility = View.VISIBLE
        }
    }


}