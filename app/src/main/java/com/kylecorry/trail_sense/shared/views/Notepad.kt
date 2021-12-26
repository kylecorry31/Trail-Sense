package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.system.Resources
import kotlin.math.max

class Notepad(context: Context, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatEditText(context, attrs) {

    private lateinit var drawer: ICanvasDrawer
    private var isSetup = false
    private val bounds = Rect()
    private var offset: Float = 0f

    init {
        inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        background = null
        isSingleLine = false
        gravity = Gravity.START or Gravity.TOP
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isSetup) {
            drawer = CanvasDrawer(context, canvas)
            setup()
            isSetup = true
        }

        drawer.canvas = canvas
        draw()
    }


    fun setup() {
        drawer.stroke(Resources.androidTextColorSecondary(context))
        drawer.noFill()
        offset = drawer.dp(4f)
        drawer.strokeWeight(drawer.dp(1f))
        drawer.opacity(60)
    }

    fun draw() {
        val count = max(height / lineHeight, lineCount)
        var baseline = getLineBounds(0, bounds)
        for (i in 0 until count) {
            drawer.line(
                bounds.left.toFloat(),
                baseline.toFloat() + offset,
                bounds.right.toFloat(),
                baseline.toFloat() + offset
            )
            baseline += lineHeight
        }
    }

}