package com.kylecorry.trail_sense.shared.views

import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes

fun View.parse(
    attrs: AttributeSet?,
    @StyleableRes res: IntArray,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0,
    block: TypedArray.() -> Unit
) {
    val arr = context.theme.obtainStyledAttributes(
        attrs,
        res,
        defStyleAttr,
        defStyleRes
    )
    arr.block()
    arr.recycle()
}