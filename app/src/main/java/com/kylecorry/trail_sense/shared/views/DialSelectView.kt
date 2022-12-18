package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.sol.math.SolMath.wrap
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import kotlin.math.absoluteValue

class DialSelectView : CanvasView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val haptics by lazy { HapticSubsystem.getInstance(context) }

    init {
        runEveryCycle = true
    }

    var options: List<String> = listOf()
        set(value) {
            field = value
            invalidate()
        }

    var selected: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var range = 180f
        set(value) {
            field = value
            invalidate()
        }

    var background = Color.BLACK
        set(value) {
            field = value
            gradient = LinearGradient(
                0f,
                0f,
                gradientLength * width,
                0f,
                value,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            invalidate()
        }

    var foreground = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    var selectedColor = AppColor.Orange.color
        set(value) {
            field = value
            invalidate()
        }

    var alignToTop = false
        set(value) {
            field = value
            invalidate()
        }

    var areHapticsEnabled = false
        set(value) {
            field = value
            if (!value){
                haptics.off()
            }
            invalidate()
        }

    private var lastSelection = 0
    private var scrollPosition = 0f
    private var targetScrollPosition = 0f
    private var tickLength = 0f
    private lateinit var gradient: LinearGradient
    private val gradientPaint: Paint = Paint()
    private val gradientLength = 0.25f

    var selectionChangeListener: (selection: Int) -> Unit = {}

    override fun setup() {
        gradient = LinearGradient(
            0f,
            0f,
            gradientLength * width,
            0f,
            background,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        gradientPaint.isDither = true
        gradientPaint.shader = gradient
        tickLength = dp(6f)
        textSize(sp(14f))
        strokeWeight(dp(1.5f))
    }

    override fun draw() {
        background(background)
        val delta = deltaAngle(scrollPosition, targetScrollPosition)
        if (delta.absoluteValue < 0.1f) {
            scrollPosition = targetScrollPosition
        } else {
            scrollPosition += 0.1f * deltaAngle(scrollPosition, targetScrollPosition)
        }
        drawDial()
        drawGradient()
    }

    private fun drawGradient() {
        canvas.drawRect(0f, 0f, width * gradientLength, height.toFloat(), gradientPaint)
        push()
        rotate(180f)
        canvas.drawRect(0f, 0f, width * gradientLength, height.toFloat(), gradientPaint)
        pop()
    }

    private fun drawDial() {
        val options = options
        val scrollPosition = scrollPosition
        if (options.isEmpty()) {
            return
        }
        val tickSpacingDegrees = 360 / (options.size * 2)
        val labelSpacingDegrees = 360 / options.size

        textMode(TextMode.Center)

        for (degree in 0 until 360 step tickSpacingDegrees) {
            val x = getPosition(scrollPosition, degree.toFloat())

            if (x < 0 || x > width) {
                continue
            }

            stroke(foreground)
            noFill()
            if (alignToTop) {
                val y = 0.25f * height
                line(x, y + tickLength / 2f, x, y - tickLength / 2f)
                if (degree % labelSpacingDegrees == 0) {
                    val idx = degree / labelSpacingDegrees
                    noStroke()
                    fill(if (selected == idx) selectedColor else foreground)
                    text(options[idx], x, height / 2f)
                }
            } else {
                val y = 0.75f * height
                line(x, y + tickLength / 2f, x, y - tickLength / 2f)
                if (degree % labelSpacingDegrees == 0) {
                    val idx = degree / labelSpacingDegrees
                    noStroke()
                    fill(if (selected == idx) selectedColor else foreground)
                    text(options[idx], x, height / 2f)
                }
            }
        }
    }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val degChange = distanceX * range / width.toFloat()
            scrollPosition = wrap(scrollPosition + degChange, 0f, 360f)
            targetScrollPosition = scrollPosition
            val degPerOption = 360f / options.size

            val nearestOption = List(options.size) { index ->
                val pos = degPerOption * index
                index to deltaAngle(pos, scrollPosition).absoluteValue
            }.minByOrNull { it.second }?.first ?: 0

            if (areHapticsEnabled && nearestOption != selected){
                haptics.tick()
            }

            selected = nearestOption
            return true
        }

    }

    private val gestureDetector = GestureDetector(context, mGestureListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP) {
            scrollToOption(selected)
            if (selected != lastSelection){
                selectionChangeListener(selected)
                lastSelection = selected
            }
        }
        invalidate()
        return true
    }


    private fun getPosition(scrollPosition: Float, degrees: Float): Float {
        val delta = deltaAngle(scrollPosition, degrees)
        return map(delta, -range / 2, range / 2, 0f, width.toFloat())
    }

    fun scrollToOption(optionIdx: Int) {
        targetScrollPosition = optionIdx * 360f / options.size
    }
}