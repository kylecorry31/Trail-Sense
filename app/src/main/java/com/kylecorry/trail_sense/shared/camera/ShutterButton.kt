package com.kylecorry.trail_sense.shared.camera

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import kotlin.math.min

class ShutterButton(context: Context, attrs: AttributeSet? = null) : CanvasView(context, attrs) {

    init {
        runEveryCycle = false
    }

    private var outlineThickness: Float = 0f
    private var isShutterPressed = false
    private var shutterColor: Int = Color.BLACK

    override fun draw() {
        val diameter = min(width, height).toFloat()
        clear()
        stroke(shutterColor)
        strokeWeight(outlineThickness)
        fill(if (isShutterPressed) shutterColor else Color.WHITE)
        circle(width / 2f, height / 2f, diameter - outlineThickness * 2)
    }

    override fun setup() {
        shutterColor = Resources.getPrimaryColor(context)
        outlineThickness = dp(4f)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN){
            isShutterPressed = true
        }

        if (event?.action == MotionEvent.ACTION_UP){
            isShutterPressed = false
        }

        invalidate()

        return super.onTouchEvent(event)
    }
}