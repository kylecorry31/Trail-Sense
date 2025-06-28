package com.kylecorry.trail_sense.tools.maps.ui

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
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.paths.ui.DistanceScale
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

    override var mapAzimuth: Float
        get() = 0f
        set(_) {
            // Do nothing
        }
    override val mapRotation: Float = 0f

    private var scale = 1f

    private val prefs by lazy { UserPreferences(context) }
    private val units by lazy { prefs.baseDistanceUnits }
    private val formatService by lazy { FormatService.getInstance(context) }

    // TODO: Extract to an overlay layer
    private val scaleBar = Path()
    private val distanceScale = DistanceScale()
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

        // TODO: Is this scale logic needed?
        maxScale = getScale(0.1f).coerceAtLeast(2 * minScale)
        zoomTo(clampScale(scale))

        drawLayers()
        drawScale()
        drawOverlays()
    }

    private fun drawLayers() {
        if (scale != lastScale) {
            lastScale = scale
            layers.forEach { it.invalidate() }
        }
        // TODO: If map bounds changed, invalidate layers

        layers.forEach { it.draw(this, this) }
    }

    // TODO: Use mercator projection
    private fun getPixels(
        location: Coordinate
    ): PixelCoordinate {
        val distance = mapCenter.distanceTo(location)
        val bearing = mapCenter.bearingTo(location)
        val angle = wrap(-(bearing.value - 90), 0f, 360f)
        val pixelDistance = distance / metersPerPixel
        val xDiff = cosDegrees(angle.toDouble()).toFloat() * pixelDistance
        val yDiff = sinDegrees(angle.toDouble()).toFloat() * pixelDistance
        return PixelCoordinate(width / 2f + xDiff, height / 2f - yDiff)
    }

    // TODO: Use mercator projection
    private fun getCoordinate(
        pixel: PixelCoordinate
    ): Coordinate {
        val xDiff = pixel.x - width / 2f
        val yDiff = height / 2f - pixel.y
        val distance = sqrt(xDiff * xDiff + yDiff * yDiff) * metersPerPixel
        val angle = normalizeAngle(atan2(yDiff, xDiff).toDegrees())
        return mapCenter.plus(Distance.meters(distance), Bearing(angle + 90))
    }

    private fun drawOverlays() {
        layers.forEach { it.drawOverlay(this, this) }
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