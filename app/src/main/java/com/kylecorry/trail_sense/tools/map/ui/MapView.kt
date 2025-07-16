package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geography.projections.MercatorProjection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import kotlin.math.max
import kotlin.math.min


class MapView(context: Context, attrs: AttributeSet? = null) : CanvasView(context, attrs),
    IMapView {
    var isInteractive = true
    var isPanEnabled = true
    var isZoomEnabled = true

    private val lookupMatrix = Matrix()

    private val layers = mutableListOf<ILayer>()
    private val hooks = Hooks()

    init {
        // TODO: Only do this if layers change - they need to be able to notify the map
        runEveryCycle = true
    }

    // TODO: Expose a method to fit to bounds (sets map center and meters per pixel)
    override val mapBounds: CoordinateBounds
        get() = hooks.memo("bounds", metersPerPixel, mapCenter, width, height, mapAzimuth != 0f) {
            // Increase size to account for 45 degree rotation
            var rotated = Rectangle(
                0f,
                height.toFloat(),
                width.toFloat(),
                0f,
            )

            if (mapAzimuth != 0f) {
                rotated = rotated.rotate(45f)
            }

            val corners = listOf(
                toCoordinate(PixelCoordinate(rotated.left, rotated.bottom)),
                toCoordinate(PixelCoordinate(rotated.right, rotated.bottom)),
                toCoordinate(PixelCoordinate(rotated.left, rotated.top)),
                toCoordinate(PixelCoordinate(rotated.right, rotated.top))
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

    override var mapAzimuth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    override val mapRotation: Float = 0f

    private var scale = 1f
    private var lastScale = 1f
    private var minScale = 0.0002f
    private var maxScale = 1f

    override fun addLayer(layer: ILayer) {
        layers.add(layer)
    }

    override fun removeLayer(layer: ILayer) {
        layers.remove(layer)
    }

    override fun setLayers(layers: List<ILayer>) {
        this.layers.clear()
        this.layers.addAll(layers)
        this.layers.filterIsInstance<IAsyncLayer>()
            .forEach { it.setHasUpdateListener { invalidate() } }
    }

    private val projection = MercatorProjection()

    override fun toPixel(coordinate: Coordinate): PixelCoordinate {
        val center = projection.toPixels(mapCenter)

        // Always render the hemispheres closest to the map center
        val newCoordinate = Coordinate(
            coordinate.latitude,
            mapCenter.longitude + deltaAngle(
                mapCenter.longitude.toFloat(),
                coordinate.longitude.toFloat()
            )
        )

        val projected = projection.toPixels(newCoordinate)

        val x = (projected.x - center.x) * (Geology.EARTH_AVERAGE_RADIUS / metersPerPixel)
        val y =
            (center.y - projected.y) * (Geology.EARTH_AVERAGE_RADIUS / metersPerPixel) // Y inverted

        return PixelCoordinate(
            x.toFloat() + width / 2f,
            y.toFloat() + height / 2f
        )
    }


    override fun toCoordinate(pixel: PixelCoordinate): Coordinate {
        val center = projection.toPixels(mapCenter)

        val x = (pixel.x - width / 2f) * metersPerPixel / Geology.EARTH_AVERAGE_RADIUS
        val y =
            (height / 2f - pixel.y) * metersPerPixel / Geology.EARTH_AVERAGE_RADIUS // Y inverted

        val projected = Vector2(
            center.x + x.toFloat(),
            center.y + y.toFloat()
        )
        return projection.toCoordinate(projected)
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

        push()
        drawer.rotate(-mapAzimuth)
        drawLayers()
        pop()
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

    private fun translatePixels(distanceX: Float, distanceY: Float) {
        val newPoint = PixelCoordinate(width / 2f + distanceX, height / 2f + distanceY)
        val newCenter = toCoordinate(newPoint)
        mapCenter = Coordinate(
            newCenter.latitude.coerceIn(-85.0, 85.0),
            Coordinate.toLongitude(newCenter.longitude)
        )
    }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (isPanEnabled) {
                translatePixels(distanceX, distanceY)
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            zoomWithFocus(2f, PixelCoordinate(e.x, e.y))
            return super.onDoubleTap(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val pixel = unrotated(PixelCoordinate(e.x, e.y))
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
            zoomWithFocus(detector.scaleFactor, PixelCoordinate(detector.focusX, detector.focusY))
            return true
        }
    }

    private fun zoomWithFocus(scaleFactor: Float, focus: PixelCoordinate) {
        if (!isZoomEnabled) return

        // Calculate the focus coordinate before zooming
        val focusCoordinate = toCoordinate(focus)

        zoom(scaleFactor)

        if (isPanEnabled) {
            // Keep the focus point stationary
            val newFocusPixel = toPixel(focusCoordinate)
            val dx = focus.x - newFocusPixel.x
            val dy = focus.y - newFocusPixel.y
            translatePixels(-dx, -dy)
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

    private fun unrotated(pixel: PixelCoordinate): PixelCoordinate {
        val point = PointF(pixel.x, pixel.y)
        return transform(point, invert = true, inPlace = true) {
            postRotate(-mapAzimuth, width / 2f, height / 2f)
        }.let { PixelCoordinate(it.x, it.y) }
    }

    private fun transform(
        point: PointF,
        invert: Boolean = false,
        inPlace: Boolean = false,
        actions: Matrix.() -> Unit
    ): PointF {
        synchronized(lookupMatrix) {
            lookupMatrix.reset()
            actions(lookupMatrix)
            if (invert) {
                lookupMatrix.invert(lookupMatrix)
            }
            val pointArray = floatArrayOf(point.x, point.y)
            lookupMatrix.mapPoints(pointArray)

            if (inPlace) {
                point.x = pointArray[0]
                point.y = pointArray[1]
                return point
            }

            return PointF(pointArray[0], pointArray[1])
        }
    }
}