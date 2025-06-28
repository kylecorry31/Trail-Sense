package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.normalizeAngle
import com.kylecorry.sol.math.SolMath.sinDegrees
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.SolMath.wrap
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


class MapView(context: Context, attrs: AttributeSet? = null) : CanvasView(context, attrs),
    IMapView {
    var isInteractive = true
    var isPanEnabled = true
    var isZoomEnabled = true

    private val layers = mutableListOf<ILayer>()
    private val hooks = Hooks()

    init {
        // TODO: Only do this if layers change - they need to be able to notify the map
        runEveryCycle = true
    }

    // TODO: Expose a method to fit to bounds (sets map center and meters per pixel)
    override val mapBounds: CoordinateBounds
        get() = hooks.memo("bounds", metersPerPixel, mapCenter, width, height) {
            val corners = listOf(
                toCoordinate(PixelCoordinate(0f, 0f)),
                toCoordinate(PixelCoordinate(width.toFloat(), 0f)),
                toCoordinate(PixelCoordinate(0f, height.toFloat())),
                toCoordinate(PixelCoordinate(width.toFloat(), height.toFloat()))
            )
            CoordinateBounds.from(corners)
        }

    override var metersPerPixel: Float
        get() {
            return 1f / scale
        }
        set(value) {
            zoomTo(getScale(value))
        }

    override val layerScale: Float
        get() = min(1f, max(scale, 0.9f))

    override var mapCenter: Coordinate = Coordinate.zero
        set(value) {
            field = value
            invalidate()
        }

    // TODO: Allow rotation
    override var mapAzimuth: Float
        get() = 0f
        set(_) {
            // Do nothing
        }
    override val mapRotation: Float = 0f

    private var scale = 1f
    private var lastScale = 1f
    private var minScale = 0f
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

    // TODO: Use mercator projection
    override fun toPixel(coordinate: Coordinate): PixelCoordinate {
        val distance = mapCenter.distanceTo(coordinate)
        val bearing = mapCenter.bearingTo(coordinate)
        val angle = wrap(-(bearing.value - 90), 0f, 360f)
        val pixelDistance = distance / metersPerPixel
        val xDiff = cosDegrees(angle.toDouble()).toFloat() * pixelDistance
        val yDiff = sinDegrees(angle.toDouble()).toFloat() * pixelDistance
        return PixelCoordinate(width / 2f + xDiff, height / 2f - yDiff)
    }

    // TODO: Use mercator projection
    override fun toCoordinate(pixel: PixelCoordinate): Coordinate {
        val xDiff = pixel.x - width / 2f
        val yDiff = height / 2f - pixel.y
        val distance = sqrt(xDiff * xDiff + yDiff * yDiff) * metersPerPixel
        val angle = normalizeAngle(atan2(yDiff, xDiff).toDegrees())
        return mapCenter.plus(Distance.meters(distance), Bearing(angle + 90))
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

        // TODO: Is this scale logic needed?
        maxScale = getScale(0.1f).coerceAtLeast(2 * minScale)
        zoomTo(clampScale(scale))

        drawLayers()
        layers.forEach { it.drawOverlay(this, this) }
    }

    private fun drawLayers() {
        if (scale != lastScale) {
            lastScale = scale
            layers.forEach { it.invalidate() }
        }
        // TODO: If map bounds changed, invalidate layers

        layers.forEach { it.draw(this, this) }
    }

    private fun getScale(metersPerPixel: Float): Float {
        return 1f / metersPerPixel
    }

    fun recenter() {
        scale = 1f
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun zoomTo(newScale: Float) {
        if (newScale == scale) {
            return
        }
        zoom(newScale / scale)
    }

    fun zoom(factor: Float) {
        val newScale = clampScale(scale * factor)
        val newFactor = newScale / scale
        scale *= newFactor
    }

    private fun clampScale(scale: Float): Float {
        return scale.coerceIn(minScale, max(2 * minScale, maxScale))
    }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (isPanEnabled) {
                val metersEast = distanceX * metersPerPixel
                val metersNorth = -distanceY * metersPerPixel

                mapCenter = mapCenter.plus(
                    metersEast.toDouble(),
                    Bearing(90f)
                ).plus(
                    metersNorth.toDouble(),
                    Bearing(0f)
                )

            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (isPanEnabled) {
                // TODO: Zoom in on the area that was tapped
//                mapCenter = toCoordinate(
//                    PixelCoordinate(
//                        e.x,
//                        e.y
//                    )
//                )
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
                    this@MapView,
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
                // TODO: Zoom on the area that was pinched
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
}