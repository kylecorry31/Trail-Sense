package com.kylecorry.trail_sense

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout


class ColorFilterConstraintLayout(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    private lateinit var mPaint: Paint

    init {
        initialize()
    }

    private fun initialize() {
        mPaint = Paint()
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (mPaint.colorFilter != null) {
            canvas.saveLayer(null, mPaint)
            super.dispatchDraw(canvas)
            canvas.restore()
        } else {
            super.dispatchDraw(canvas)
        }
    }

    fun setColorFilter(filter: ColorFilter?){
        mPaint.colorFilter = filter
    }

}