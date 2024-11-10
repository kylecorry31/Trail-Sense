package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MarqueeTextView(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {

    init {
        isSingleLine = true
        isFocusable = true
        isFocusableInTouchMode = true
        ellipsize = TextUtils.TruncateAt.MARQUEE
        marqueeRepeatLimit = -1
        isHorizontalFadingEdgeEnabled = true
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (focused) {
            super.onFocusChanged(true, direction, previouslyFocusedRect)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (hasWindowFocus) {
            super.onWindowFocusChanged(true)
        }
    }

    override fun isFocused(): Boolean {
        return true
    }
}