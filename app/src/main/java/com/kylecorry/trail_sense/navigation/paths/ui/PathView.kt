package com.kylecorry.trail_sense.navigation.paths.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.normalizeAngle
import com.kylecorry.sol.math.SolMath.sinDegrees
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.SolMath.wrap
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.navigation.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


class PathView(context: Context, attrs: AttributeSet? = null) : CanvasView(context, attrs),
    IMapView {
    var isInteractive = false
    var isPanEnabled = true
    var isZoomEnabled = true

    private val layers = mutableListOf<ILayer>()
    var bounds: CoordinateBounds? = null
        set(value) {
            field = value
            invalidate()
        }

    override var metersPerPixel: Float
        get() {
            val initial = getInitialScale() ?: return 1f
            return initial / scale
        }
        set(value) {
            zoomTo(getScale(value))
        }

    override val layerScale: Float
        get() = min(1f, max(scale, 0.9f))

    override var mapCenter: Coordinate
        get() = center
        set(value) {
            center = value
        }

    override var mapRotation: Float
        get() = 0f
        set(_) {
            // Do nothing
        }

    private var center: Coordinate = Coordinate.zero

    private var translateX = 0f
    private var translateY = 0f
    private var scale = 1f

    private val prefs by lazy { UserPreferences(context) }
    private val units by lazy { prefs.baseDistanceUnits }
    private val formatService by lazy { FormatService(context) }
    private val scaleBar = Path()
    private val distanceScale = DistanceScale()
    private var lastScale = 1f

    private var minScale = 0.1f
    private var maxScale = 1f

    init {
        runEveryCycle = true
    }

    override fun addLayer(layer: ILayer) {
        layers.add(layer)
    }

    override fun removeLayer(layer: ILayer) {
        layers.remove(layer)
    }

    override fun setLayers(layers: List<ILayer>) {
        this.layers.clear()
        this.layers.addAll(layers)
    }

    override fun toPixel(coordinate: Coordinate): PixelCoordinate {
        return getPixels(coordinate)
    }

    override fun toCoordinate(pixel: PixelCoordinate): Coordinate {
        return getCoordinate(pixel)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layers.forEach { it.invalidate() }
    }

    override fun setup() {
        recenter()
    }

    override fun draw() {
        clear()

        maxScale = getScale(0.1f).coerceAtLeast(2 * minScale)
        zoomTo(clampScale(scale))

        drawLayers()
        drawScale()
    }

    private fun drawLayers() {
        val bounds = bounds ?: return
        center = bounds.center

        if (scale != lastScale) {
            lastScale = scale
            layers.forEach { it.invalidate() }
        }

        layers.forEach { it.draw(this, this) }
    }

    private fun getInitialScale(): Float? {
        val bounds = bounds ?: return null
        val distanceX = bounds.width().meters().distance
        val distanceY = bounds.height().meters().distance

        if (distanceX == 0f || distanceY == 0f) {
            return null
        }

        val h = height.toFloat() - dp(32f)
        val w = width.toFloat() - dp(32f)

        return 1 / SolMath.scaleToFit(distanceX, distanceY, w, h)
    }

    private fun getPixels(
        location: Coordinate
    ): PixelCoordinate {
        val distance = center.distanceTo(location)
        val bearing = center.bearingTo(location)
        val angle = wrap(-(bearing.value - 90), 0f, 360f)
        val pixelDistance = distance / metersPerPixel
        val xDiff = cosDegrees(angle.toDouble()).toFloat() * pixelDistance
        val yDiff = sinDegrees(angle.toDouble()).toFloat() * pixelDistance
        return toView(PixelCoordinate(width / 2f + xDiff, height / 2f - yDiff))
    }

    private fun getCoordinate(
        pixel: PixelCoordinate
    ): Coordinate {
        val xDiff = pixel.x - width / 2f
        val yDiff = height / 2f - pixel.y
        val distance = sqrt(xDiff * xDiff + yDiff * yDiff) * metersPerPixel
        val angle = normalizeAngle(atan2(yDiff, xDiff).toDegrees())
        return center.plus(Distance.meters(distance), Bearing(angle + 90))
    }

    private fun drawScale() {
        noFill()
        stroke(Color.WHITE)
        strokeWeight(4f)

        val scaleSize = distanceScale.getScaleDistance(units, width / 2f, metersPerPixel)

        scaleBar.reset()
        distanceScale.getScaleBar(scaleSize, metersPerPixel, scaleBar)
        val start = width - dp(16f) - pathWidth(scaleBar)
        val y = height - dp(16f)
        push()
        translate(start, y)
        path(scaleBar)
        pop()

        textMode(TextMode.Corner)
        textSize(sp(12f))
        noStroke()
        fill(Color.WHITE)
        val scaleText =
            formatService.formatDistance(scaleSize, Units.getDecimalPlaces(scaleSize.units), false)
        text(
            scaleText,
            start - textWidth(scaleText) - dp(4f),
            y + textHeight(scaleText) / 2
        )
    }

    private fun getScale(metersPerPixel: Float): Float {
        val fullScale = getInitialScale() ?: 1f
        return fullScale / metersPerPixel
    }

    fun recenter() {
        translateX = 0f
        translateY = 0f
        scale = 1f
    }

    fun zoomTo(newScale: Float){
        if (newScale == scale){
            return
        }
        zoom(newScale / scale)
    }

    fun zoom(factor: Float) {
        val newScale = clampScale(scale * factor)
        val newFactor = newScale / scale
        scale *= newFactor
        translateX *= newFactor
        translateY *= newFactor
    }

    private fun clampScale(scale: Float): Float {
        return scale.coerceIn(minScale, max(2 * minScale, maxScale))
    }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // TODO: Keep path on screen
            if (isPanEnabled) {
                translateX -= distanceX
                translateY -= distanceY
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (isPanEnabled) {
                translateX += width / 2f - e.x
                translateY += height / 2f - e.y
            }

            if (isZoomEnabled) {
                zoom(2F)
            }

            return super.onDoubleTap(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val pixel = PixelCoordinate(e.x, e.y)
            for (layer in layers.reversed()) {
                val handled = layer.onClick(
                    drawer,
                    this@PathView,
                    pixel
                )
                if (handled) {
                    break
                }
            }

            return super.onSingleTapConfirmed(e)
        }
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (isZoomEnabled) {
                zoom(detector.scaleFactor)
            }
            return true
        }
    }

    private val gestureDetector = GestureDetector(context, mGestureListener)
    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isInteractive) {
            mScaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
        }
        return true
    }

    private fun toView(source: PixelCoordinate): PixelCoordinate {
        return PixelCoordinate(
            source.x + translateX,
            source.y + translateY
        )
    }

    private fun toSource(screen: PixelCoordinate): PixelCoordinate {
        return PixelCoordinate(
            screen.x - translateX,
            screen.y - translateY
        )
    }

}