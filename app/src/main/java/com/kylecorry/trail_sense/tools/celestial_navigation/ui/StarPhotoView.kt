package com.kylecorry.trail_sense.tools.celestial_navigation.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Shows the captured photo (pinch to zoom, drag to pan) with the detected and matched stars.
 * Taps are reported in full resolution image coordinates.
 */
class StarPhotoView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    data class Marker(val x: Double, val y: Double, val label: String?, val color: Int)

    var bitmap: Bitmap? = null
        set(value) {
            field = value
            resetTransform()
            invalidate()
        }

    // Full resolution image size (the bitmap may be downscaled)
    var imageWidth = 1
    var imageHeight = 1

    var markers: List<Marker> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var onTap: ((x: Double, y: Double) -> Unit)? = null

    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f
    private val drawMatrix = Matrix()
    private val inverse = Matrix()

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
    }

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val point = floatArrayOf(e.x, e.y)
            inverse.mapPoints(point)
            val scale = imageWidth.toFloat() / (bitmap?.width ?: 1)
            onTap?.invoke(point[0].toDouble() * scale, point[1].toDouble() * scale)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            panX -= distanceX
            panY -= distanceY
            invalidate()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            resetTransform()
            invalidate()
            return true
        }
    })

    private val scaleGestures = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newZoom = (zoom * detector.scaleFactor).coerceIn(1f, 20f)
            val factor = newZoom / zoom
            panX = detector.focusX - (detector.focusX - panX) * factor
            panY = detector.focusY - (detector.focusY - panY) * factor
            zoom = newZoom
            invalidate()
            return true
        }
    })

    private fun resetTransform() {
        zoom = 1f
        panX = 0f
        panY = 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return
        val fit = min(width / bmp.width.toFloat(), height / bmp.height.toFloat())
        val offsetX = (width - bmp.width * fit) / 2
        val offsetY = (height - bmp.height * fit) / 2
        drawMatrix.reset()
        drawMatrix.postScale(fit, fit)
        drawMatrix.postTranslate(offsetX, offsetY)
        drawMatrix.postScale(zoom, zoom)
        drawMatrix.postTranslate(panX, panY)
        drawMatrix.invert(inverse)
        canvas.drawBitmap(bmp, drawMatrix, null)

        val scale = bmp.width.toFloat() / imageWidth
        val radius = max(12f, 10f * zoom)
        for (marker in markers) {
            val point = floatArrayOf(marker.x.toFloat() * scale, marker.y.toFloat() * scale)
            drawMatrix.mapPoints(point)
            circlePaint.color = marker.color
            canvas.drawCircle(point[0], point[1], radius, circlePaint)
            marker.label?.let {
                textPaint.color = marker.color
                canvas.drawText(it, point[0] + radius + 4, point[1] + 10, textPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestures.onTouchEvent(event)
        gestures.onTouchEvent(event)
        return true
    }

    companion object {
        val DETECTED = Color.YELLOW
        val MATCHED = Color.GREEN
    }
}
