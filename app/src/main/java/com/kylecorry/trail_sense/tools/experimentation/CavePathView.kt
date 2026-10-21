package com.kylecorry.trail_sense.tools.experimentation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Orthographic 3D view of the path. One finger rotates, two fingers pan and pinch zooms, double tap
 * toggles 4x vertical exaggeration.
 */
class CavePathView : CanvasView {

    var reckoner: DeadReckoner? = null

    private var yaw = 30f
    private var pitch = 55f
    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f
    private var verticalScale = 1f

    private var textColor = Color.WHITE
    private var lastX = 0f
    private var lastY = 0f
    private var lastPointerCount = 0

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                zoom = (zoom * detector.scaleFactor).coerceIn(0.2f, 20f)
                return true
            }
        })

    private val tapDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                verticalScale = if (verticalScale == 1f) 4f else 1f
                return true
            }
        })

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        tapDetector.onTouchEvent(event)

        val count = event.pointerCount
        var x = 0f
        var y = 0f
        for (i in 0 until count) {
            x += event.getX(i) / count
            y += event.getY(i) / count
        }

        if (event.actionMasked == MotionEvent.ACTION_MOVE && count == lastPointerCount) {
            val dx = x - lastX
            val dy = y - lastY
            if (count == 1) {
                yaw += dx * 0.5f
                pitch = (pitch + dy * 0.5f).coerceIn(0f, 90f)
            } else {
                panX += dx
                panY += dy
            }
        }
        lastX = x
        lastY = y
        lastPointerCount = if (event.actionMasked == MotionEvent.ACTION_UP) 0 else count
        parent?.requestDisallowInterceptTouchEvent(true)
        return true
    }

    override fun setup() {
        textColor = Resources.androidTextColorPrimary(context)
        textSize(sp(14f))
        textMode(TextMode.Center)
    }

    override fun draw() {
        background(Color.TRANSPARENT)
        val points = reckoner?.path?.toList() ?: return
        if (points.isEmpty()) return

        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())
        val cosY = cos(yawRad).toFloat()
        val sinY = sin(yawRad).toFloat()
        val cosP = cos(pitchRad).toFloat()
        val sinP = sin(pitchRad).toFloat()

        val minUp = points.minOf { it.up }
        val maxUp = points.maxOf { it.up }
        val minE = points.minOf { it.east }
        val maxE = points.maxOf { it.east }
        val minN = points.minOf { it.north }
        val maxN = points.maxOf { it.north }
        val centerE = (minE + maxE) / 2
        val centerN = (minN + maxN) / 2
        val centerUp = (minUp + maxUp) / 2

        // Radius of the bounding box, with a minimum so the path doesn't jump around early on
        val radius = max(
            10f,
            sqrt(
                (maxE - minE) * (maxE - minE) + (maxN - minN) * (maxN - minN) +
                        (maxUp - minUp) * (maxUp - minUp) * verticalScale * verticalScale
            ) / 2
        )
        val scale = minOf(width, height) / 2f * 0.85f / radius * zoom

        fun sx(e: Float, n: Float): Float {
            return (e - centerE) * cosY + (n - centerN) * sinY
        }

        fun depth(e: Float, n: Float): Float {
            return -(e - centerE) * sinY + (n - centerN) * cosY
        }

        fun px(e: Float, n: Float) = width / 2f + panX + sx(e, n) * scale
        fun py(e: Float, n: Float, up: Float): Float {
            val screenUp = (up - centerUp) * verticalScale * cosP + depth(e, n) * sinP
            return height / 2f + panY - screenUp * scale
        }

        fun heightColor(up: Float): Int {
            val t = ((up - minUp) / max(maxUp - minUp, 5f)).coerceIn(0f, 1f)
            return Color.rgb((255 * t).toInt(), (80 + 100 * (1 - abs(2 * t - 1))).toInt(), (255 * (1 - t)).toInt())
        }

        // Shadow of the path on the lowest level, to make height easier to judge
        stroke(Color.GRAY)
        strokeWeight(2f)
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            line(px(a.east, a.north), py(a.east, a.north, minUp), px(b.east, b.north), py(b.east, b.north, minUp))
        }

        // Drop lines every 20 points
        for (i in points.indices step 20) {
            val p = points[i]
            line(px(p.east, p.north), py(p.east, p.north, minUp), px(p.east, p.north), py(p.east, p.north, p.up))
        }

        strokeWeight(6f)
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            stroke(heightColor(b.up))
            line(px(a.east, a.north), py(a.east, a.north, a.up), px(b.east, b.north), py(b.east, b.north, b.up))
        }

        noStroke()
        val first = points.first()
        val last = points.last()
        fill(Color.GREEN)
        circle(px(first.east, first.north), py(first.east, first.north, first.up), dp(14f))
        fill(Color.RED)
        circle(px(last.east, last.north), py(last.east, last.north, last.up), dp(14f))

        // North arrow from the middle of the path
        stroke(textColor)
        strokeWeight(3f)
        val ax = px(centerE, centerN)
        val ay = py(centerE, centerN, minUp)
        val nx = px(centerE, centerN + radius * 0.3f)
        val ny = py(centerE, centerN + radius * 0.3f, minUp)
        line(ax, ay, nx, ny)
        noStroke()
        fill(textColor)
        text("N", nx, ny - dp(8f))

        text("Up %.1f to %.1f m (x%.0f vertical)".format(minUp, maxUp, verticalScale), width / 2f, height - dp(8f))
    }
}
