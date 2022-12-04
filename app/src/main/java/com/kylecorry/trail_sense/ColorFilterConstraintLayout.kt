package com.kylecorry.trail_sense

import android.content.Context
import android.graphics.ColorFilter
import android.graphics.Paint
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout


class ColorFilterConstraintLayout(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    private var mPaint = Paint()

    fun setColorFilter(filter: ColorFilter?) {
        mPaint.colorFilter = filter
        if (filter != null) {
            setLayerType(LAYER_TYPE_HARDWARE, mPaint)
        } else {
            setLayerType(LAYER_TYPE_NONE, mPaint)
        }
    }

}