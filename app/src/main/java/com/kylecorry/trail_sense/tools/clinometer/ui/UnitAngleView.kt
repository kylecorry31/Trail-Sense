package com.kylecorry.trail_sense.tools.clinometer.ui

import android.content.Context
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.system.Resources
import kotlin.math.min

class UnitAngleView : CanvasView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        runEveryCycle = false
    }

    var angle = 0f
        set(value) {
            field = value
            invalidate()
        }


    override fun setup() {
        stroke(Resources.androidTextColorPrimary(context))
        strokeWeight(dp(2f))
    }

    override fun draw() {
        val radius = min(width.toFloat(), height.toFloat()) / 2
        line(width / 2f - radius, height / 2f, width / 2f + radius, height / 2f)
        push()
        rotate(angle - 180)
        line(width / 2f, height / 2f, width / 2f + radius, height / 2f)
        pop()
    }


}