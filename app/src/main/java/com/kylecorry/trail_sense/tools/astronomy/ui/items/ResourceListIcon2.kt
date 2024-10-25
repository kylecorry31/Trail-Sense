package com.kylecorry.trail_sense.tools.astronomy.ui.items

import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.views.image.AsyncImageView
import com.kylecorry.andromeda.views.list.ListIcon
import kotlin.math.roundToInt

data class ResourceListIcon2(
    @DrawableRes val id: Int,
    @ColorInt val tint: Int? = null,
    @DrawableRes val backgroundId: Int? = null,
    @ColorInt val backgroundTint: Int? = null,
    val size: Float = 24f,
    val foregroundSize: Float = size,
    val clipToBackground: Boolean = false,
    val rotation: Float = 0f,
    val scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER,
    val onClick: (() -> Unit)? = null
) : ListIcon {
    override fun apply(image: ImageView) {
        image.isVisible = true
        image.setImageResource(id)
        if (image is AsyncImageView) {
            image.recycleLastBitmap(false)
        }
        Colors.setImageColor(image, tint)

        image.scaleType = scaleType
        tryOrLog {
            image.layoutParams.width = Resources.dp(image.context, size).toInt()
            image.layoutParams.height = Resources.dp(image.context, size).toInt()
        }

        if (backgroundId != null) {
            image.setBackgroundResource(backgroundId)
            backgroundTint?.let {
                Colors.setImageColor(image.background, it)
            }
        } else {
            image.background = null
        }

        if (clipToBackground) {
            image.outlineProvider = ViewOutlineProvider.BACKGROUND
            image.clipToOutline = true
        } else {
            image.clipToOutline = false
        }

        image.rotation = rotation

        val padding = Resources.dp(image.context, (size - foregroundSize)) / 2f
        image.setPadding(padding.roundToInt())

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
        Colors.setImageColor(text, tint)
        // TODO: Possibly apply background color
    }
}