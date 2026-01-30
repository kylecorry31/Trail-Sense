package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.OverScroller
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.math.SolMath.sinDegrees
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.math.optimization.Optimization
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.science.geography.projections.MercatorProjection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.map_layers.MapViewLayerManager
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toCoordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


class MapView(context: Context, attrs: AttributeSet? = null) : CanvasView(context, attrs),
    IMapView {
    var isInteractive = true
    var isPanEnabled = true
    var isZoomEnabled = true
    var isFlingEnabled = true

    private val density = context.resources.displayMetrics.density
    private val scroller = OverScroller(context)
    private var lastFlingX = 0
    private var lastFlingY = 0

    private val lookupMatrix = Matrix()

    override val layerManager = MapViewLayerManager {
        post { invalidate() }
    }
    private val hooks = Hooks()

    private var onLongPressCallback: ((Coordinate) -> Unit)? = null
    private var onScaleChange: ((resolutionPixels: Float) -> Unit)? = null
    private var onCenterChange: ((center: Coordinate) -> Unit)? = null

    override var userLocation: Coordinate = Coordinate.zero
        set(value) {
            field = value
            invalidate()
        }

    override var userLocationAccuracy: Distance? = null
        set(value) {
            field = value
            invalidate()
        }

    override var userAzimuth: Bearing = Bearing.from(0f)
        set(value) {
            field = value
            invalidate()
        }

    init {
        runEveryCycle = false
    }

    fun setOnLongPressListener(callback: ((Coordinate) -> Unit)?) {
        onLongPressCallback = callback
    }

    override val mapBounds: CoordinateBounds
        get() = hooks.memo(
            "bounds",
            this@MapView.resolutionPixels, mapCenter, width, height, mapAzimuth != 0f
        ) {
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

    override var resolution: Float
        get() = this@MapView.resolutionPixels * density
        set(value) {
            this@MapView.resolutionPixels = value / density
        }

    override val zoom: Float
        get() = TileMath.getZoomLevel(mapCenter, resolution)

    override var resolutionPixels: Float
        get() {
            return (1f / scale) * MercatorProjection.getScaleForLatitude(mapCenter.latitude)
        }
        set(value) {
            zoomTo(getScale(value / (MercatorProjection.getScaleForLatitude(mapCenter.latitude))))
        }

    private val equatorialResolutionPixels: Float
        get() = 1f / scale

    override val layerScale: Float
        get() = min(1f, max(scale, 0.9f))

    override var mapCenter: Coordinate = Coordinate.zero
        set(value) {
            field = Coordinate(
                value.latitude.coerceIn(constraintBounds.south, constraintBounds.north),
                Coordinate.toLongitude(value.longitude).coerceIn(
                    min(constraintBounds.west, constraintBounds.east),
                    max(constraintBounds.west, constraintBounds.east)
                )
            )
            onCenterChange?.invoke(field)
            invalidate()
        }

    private val mapCenterPixels: Vector2
        get() = hooks.memo("map_center_pixels", mapCenter, projection) {
            projection.toPixels(mapCenter)
        }

    override var mapAzimuth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    override val mapRotation: Float = 0f

    var scale = 1f
        private set(value) {
            field = value
            onScaleChange?.invoke(this@MapView.resolutionPixels)
            invalidate()
        }
    private var lastScale = 1f
    var minScale = 0.0002f
        set(value) {
            field = value
            if (scale < minScale) {
                zoomTo(minScale)
            }
            invalidate()
        }

    var constraintBounds: CoordinateBounds = CoordinateBounds(85.0, 180.0, -85.0, -180.0)
        set(value) {
            field = value
            if (!field.contains(mapCenter)) {
                mapCenter = field.center
            }
            invalidate()
        }

    var projection: IMapProjection = MercatorProjection()
        set(value) {
            field = value
            this@MapView.layerManager.invalidate()
            invalidate()
        }


    private var maxScale = 1f
    private var isScaling = false

    private var fitToViewBounds: CoordinateBounds? = null
    private var fitToViewPadding: Float = 1f

    override val mapProjection: IMapViewProjection
        get() = hooks.memo(
            "mapProjection",
            mapCenter,
            mapCenterPixels,
            projection,
            equatorialResolutionPixels,
            width,
            height,
            zoom,
            resolution
        ) {
            val mapCenter = mapCenter
            val center = mapCenterPixels
            val projection = projection
            val equatorialResolutionPixels = this@MapView.equatorialResolutionPixels
            val groundResolutionPixels =
                equatorialResolutionPixels * MercatorProjection.getScaleForLatitude(mapCenter.latitude)
            val width = width
            val height = height
            val zoom = zoom
            val resolution = resolution

            object : IMapProjection, IMapViewProjection {
                override fun toPixels(
                    latitude: Double,
                    longitude: Double
                ): PixelCoordinate {


                    // Always render the hemispheres closest to the map center
                    val projected = projection.toPixels(
                        latitude, mapCenter.longitude + deltaAngle(
                            mapCenter.longitude.toFloat(),
                            longitude.toFloat()
                        )
                    )

                    val x =
                        (projected.x - center.x) * (TileMath.WEB_MERCATOR_RADIUS / equatorialResolutionPixels)
                    val y =
                        (center.y - projected.y) * (TileMath.WEB_MERCATOR_RADIUS / equatorialResolutionPixels) // Y inverted

                    return PixelCoordinate(
                        x.toFloat() + width / 2f,
                        y.toFloat() + height / 2f
                    )
                }

                override fun toCoordinate(pixel: Vector2): Coordinate {
                    val x =
                        (pixel.x - width / 2f) * equatorialResolutionPixels / TileMath.WEB_MERCATOR_RADIUS
                    val y =
                        (height / 2f - pixel.y) * equatorialResolutionPixels / TileMath.WEB_MERCATOR_RADIUS // Y inverted

                    val projected = Vector2(
                        center.x + x.toFloat(),
                        center.y + y.toFloat()
                    )
                    return projection.toCoordinate(projected)
                }

                override fun toPixels(location: Coordinate): PixelCoordinate {
                    return toPixels(location.latitude, location.longitude)
                }

                override val resolutionPixels: Float = groundResolutionPixels
                override val resolution: Float = resolution
                override val zoom: Float = zoom
                override val center: Coordinate = mapCenter
            }
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this@MapView.layerManager.invalidate()
    }

    override fun setup() {
        recenter()
    }

    override fun draw() {
        if (scroller.computeScrollOffset()) {
            val dx = lastFlingX - scroller.currX
            val dy = lastFlingY - scroller.currY
            lastFlingX = scroller.currX
            lastFlingY = scroller.currY
            translatePixels(dx.toFloat(), dy.toFloat())
            invalidate()
        }

        clear()

        // TODO: Is this scale logic needed?
        maxScale = getScale(0.1f).coerceAtLeast(2 * minScale)
        zoomTo(clampScale(scale))

        push()
        drawer.rotate(-mapAzimuth)
        drawLayers()
        pop()
        layerManager.drawOverlay(context, this, this)
    }

    private fun drawLayers() {
        if (scale != lastScale) {
            lastScale = scale
            this@MapView.layerManager.invalidate()
        }
        // TODO: If map bounds changed, invalidate layers

        layerManager.draw(context, this, this)
    }

    private fun getScale(resolutionPixels: Float): Float {
        return 1f / resolutionPixels
    }

    fun recenter(ignoreFitToViewBounds: Boolean = false) {
        val fitToViewBounds = fitToViewBounds
        if (ignoreFitToViewBounds || fitToViewBounds == null) {
            scale = 1f
        } else {
            fitIntoView(fitToViewBounds, fitToViewPadding)
        }
        invalidate()
    }

    // TODO: This doesn't work for world maps
    fun fitIntoView(bounds: CoordinateBounds, paddingFactor: Float = 1.25f) {
        if (width == 0 || height == 0) {
            return
        }
        fitToViewBounds = bounds
        fitToViewPadding = paddingFactor
        Optimization.newtonRaphsonIteration(scale, 0.001f, 10) {
            mapCenter = bounds.center
            val nePixel = toPixel(bounds.northEast)
            val sePixel = toPixel(bounds.southEast)
            val nwPixel = toPixel(bounds.northWest)
            val swPixel = toPixel(bounds.southWest)
            val minX = minOf(nePixel.x, sePixel.x, nwPixel.x, swPixel.x)
            val maxX = maxOf(nePixel.x, sePixel.x, nwPixel.x, swPixel.x)
            val minY = minOf(nePixel.y, sePixel.y, nwPixel.y, swPixel.y)
            val maxY = maxOf(nePixel.y, sePixel.y, nwPixel.y, swPixel.y)
            val boxWidth = (maxX - minX).absoluteValue * paddingFactor
            val boxHeight = (maxY - minY).absoluteValue * paddingFactor

            if (boxWidth == 0f || boxHeight == 0f) {
                return@newtonRaphsonIteration 0f
            }

            val scaleX = width / boxWidth
            val scaleY = height / boxHeight
            val newScale = min(scaleX, scaleY)
            zoom(newScale)
            newScale
        }
        invalidate()
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
        invalidate()
    }

    private fun clampScale(scale: Float): Float {
        return scale.coerceIn(minScale, max(2 * minScale, maxScale))
    }

    private fun translatePixels(distanceX: Float, distanceY: Float) {
        val newPoint = PixelCoordinate(width / 2f + distanceX, height / 2f + distanceY)
        mapCenter = toCoordinate(newPoint)
    }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (isPanEnabled) {
                val angle = mapAzimuth
                val dx = distanceX * cosDegrees(angle) - distanceY * sinDegrees(angle)
                val dy = distanceX * sinDegrees(angle) + distanceY * cosDegrees(angle)
                translatePixels(dx, dy)
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            zoomWithFocus(2f, PixelCoordinate(e.x, e.y))
            return super.onDoubleTap(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val pixel = unrotated(PixelCoordinate(e.x, e.y))
            layerManager.onClick(this@MapView, this@MapView, pixel)
            return super.onSingleTapConfirmed(e)
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (isPanEnabled && isFlingEnabled) {
                val angle = mapAzimuth
                val vx = velocityX * cosDegrees(angle) - velocityY * sinDegrees(angle)
                val vy = velocityX * sinDegrees(angle) + velocityY * cosDegrees(angle)
                val startX = FLING_MULTIPLIER / 2
                val startY = FLING_MULTIPLIER / 2
                lastFlingX = startX
                lastFlingY = startY
                scroller.fling(
                    startX, startY,
                    (vx * FLING_VELOCITY_SCALE).toInt(), (vy * FLING_VELOCITY_SCALE).toInt(),
                    0, FLING_MULTIPLIER,
                    0, FLING_MULTIPLIER
                )
                invalidate()
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)

            if (isScaling) return

            val pixel = unrotated(PixelCoordinate(e.x, e.y))
            val location = toCoordinate(pixel)
            onLongPressCallback?.invoke(location)
        }
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            return super.onScaleBegin(detector)
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoomWithFocus(detector.scaleFactor, PixelCoordinate(detector.focusX, detector.focusY))
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
            super.onScaleEnd(detector)
        }
    }

    private fun zoomWithFocus(scaleFactor: Float, focus: PixelCoordinate) {
        if (!isZoomEnabled) return

        // Calculate the focus coordinate before zooming
        val unrotatedFocus = unrotated(focus)
        val focusCoordinate = toCoordinate(unrotatedFocus)

        zoom(scaleFactor)

        if (isPanEnabled) {
            // Keep the focus point stationary
            val newFocusPixel = toPixel(focusCoordinate)
            val dx = unrotatedFocus.x - newFocusPixel.x
            val dy = unrotatedFocus.y - newFocusPixel.y
            translatePixels(-dx, -dy)
        }
    }

    private val gestureDetector = GestureDetector(context, mGestureListener)
    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isInteractive) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                scroller.forceFinished(true)
            }
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

    fun setOnScaleChangeListener(callback: ((resolutionPixels: Float) -> Unit)?) {
        onScaleChange = callback
    }

    fun setOnCenterChangeListener(callback: ((center: Coordinate) -> Unit)?) {
        onCenterChange = callback
    }

    override fun setOnGeoJsonFeatureClickListener(listener: ((GeoJsonFeature) -> Unit)?) {
        layerManager.setOnGeoJsonFeatureClickListener(listener)
    }

    companion object {
        private const val FLING_MULTIPLIER = 1000000
        private const val FLING_VELOCITY_SCALE = 0.6f
    }
}