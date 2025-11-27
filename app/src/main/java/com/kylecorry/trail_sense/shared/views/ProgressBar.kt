package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor

class ProgressBar : CanvasView {
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

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var progressColor: Int = Resources.getPrimaryColor(context)
        set(value) {
            field = value
            invalidate()
        }

    var trackOpacity: Int = 100
        set(value) {
            field = value
            invalidate()
        }

    var borderRadiusDp: Float = 4f
        set(value) {
            field = value
            invalidate()
        }

    override fun setup() {
    }

    override fun draw() {
        val borderRadiusPx = dp(borderRadiusDp)
        fill(progressColor)
        opacity(trackOpacity)
        rect(0f, 0f, width.toFloat(), height.toFloat(), borderRadiusPx)
        opacity(255)
        rect(0f, 0f, width * progress, height.toFloat(), borderRadiusPx)
    }
}