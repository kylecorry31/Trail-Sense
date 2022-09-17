package com.kylecorry.trail_sense.weather.ui.clouds

import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.ceres.list.ListIcon
import com.kylecorry.trail_sense.R

// TODO: Move to Ceres
data class ClippedResourceListIcon(
    @DrawableRes val id: Int,
    @ColorInt val tint: Int? = null,
    val size: Float = 24f,
    @DrawableRes val background: Int = R.drawable.circle,
    val onClick: (() -> Unit)? = null
) : ListIcon {
    override fun apply(image: ImageView) {
        image.isVisible = true
        image.setImageResource(id)
        Colors.setImageColor(image, tint)
        image.scaleType = ImageView.ScaleType.FIT_CENTER
        image.layoutParams.width = Resources.dp(image.context, size).toInt()
        image.layoutParams.height = Resources.dp(image.context, size).toInt()
        image.setBackgroundResource(background)
        image.outlineProvider = ViewOutlineProvider.BACKGROUND
        image.clipToOutline = true

        image.requestLayout()
        if (onClick == null) {
            image.setOnClickListener(null)
        } else {
            image.setOnClickListener { onClick.invoke() }
        }
    }

    override fun apply(text: TextView) {
        text.setCompoundDrawables(
            Resources.dp(text.context, 12f).toInt(),
            left = id
        )
    }
}