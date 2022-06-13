package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geology.projections.IMapProjection
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.PathLineDrawerFactory
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.RenderedPath
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.RenderedPathFactory
import com.kylecorry.trail_sense.navigation.ui.IMappableLocation
import com.kylecorry.trail_sense.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.navigation.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import kotlin.math.max
import kotlin.math.min


class OfflineMapView : SubsamplingScaleImageView, IMapView {

    var onMapLongClick: ((coordinate: Coordinate) -> Unit)? = null
    var onLocationClick: ((location: IMappableLocation) -> Unit)? = null
    var onMapClick: ((percent: PercentCoordinate) -> Unit)? = null

    private lateinit var drawer: ICanvasDrawer
    private var isSetup = false
    private var myLocation: Coordinate? = null
    private var map: Map? = null
    private val mapPath = Path()
    private var projection: IMapProjection? = null

    private val layers = mutableListOf<ILayer>()

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
        return getPixelCoordinate(coordinate, nullIfOffMap = false) ?: PixelCoordinate(0f, 0f)
    }

    fun toCoordinate(pixel: PixelCoordinate): Coordinate {
        val source = viewToSourceCoord(pixel.x, pixel.y) ?: return Coordinate.zero
        return projection?.toCoordinate(Vector2(source.x, source.y)) ?: Coordinate.zero
    }

    private fun toPixel(point: PointF): PixelCoordinate {
        return PixelCoordinate(point.x, point.y)
    }

    private fun toPoint(pixel: PixelCoordinate): PointF {
        return PointF(pixel.x, pixel.y)
    }

    override var metersPerPixel: Float
        get() = TODO("Not yet implemented")
        set(value) {}

    override var centerLocation: Coordinate
        get() = toCoordinate(center?.let { toPixel(it) } ?: PixelCoordinate(
            width / 2f,
            height / 2f
        ))
        set(value) {
            setScaleAndCenter(scale, toPoint(toPixel(value)))
        }
    override var mapRotation: Float
        get() = rotation
        set(value) {
            rotation = mapRotation
            invalidate()
        }

    var azimuth: Bearing = Bearing(0f)
        set(value) {
            field = value
            invalidate()
        }

    override val layerScale: Float
        get() = min(1f, max(scale, 0.9f))

    private var locations = emptyList<IMappableLocation>()
    private var paths = emptyList<IMappablePath>()
    private var pathPool = ObjectPool { Path() }
    private var renderedPaths = mapOf<Long, RenderedPath>()
    private var pathsRendered = false
    private var lastScale = 1f
    private var highlightedLocation: IMappableLocation? = null
    private var showCalibrationPoints = false

    private var lastImage: String? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (!isReady || canvas == null) {
            return
        }

        if (!isSetup) {
            drawer = CanvasDrawer(context, canvas)
            setup()
            isSetup = true
        }

        drawer.canvas = canvas
        draw()
    }

    fun setup() {
        setPanLimit(PAN_LIMIT_OUTSIDE)
        maxScale = 6f
    }

    fun draw() {
        map ?: return

        // TODO: This only needs to be changed when the scale or translate changes
        mapPath.apply {
            rewind()
            val topLeft = sourceToViewCoord(0f, 0f)!!
            val bottomRight = sourceToViewCoord(realWidth.toFloat(), realHeight.toFloat())!!
            addRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, Path.Direction.CW)
        }

        if (scale != lastScale) {
            pathsRendered = false
            lastScale = scale
        }

        drawCalibrationPoints()

        if (map?.calibrationPoints?.size == 2) {
            layers.forEach { it.draw(drawer, this) }
            drawPaths()
            drawLocations()
            drawMyLocation()
        }
    }

    fun showMap(map: Map) {
        if (orientation != map.rotation) {
            orientation = when (map.rotation) {
                90 -> ORIENTATION_90
                180 -> ORIENTATION_180
                270 -> ORIENTATION_270
                else -> ORIENTATION_0
            }
        }
        if (lastImage != map.filename) {
            val file = LocalFiles.getFile(context, map.filename, false)
            setImage(ImageSource.uri(file.toUri()))
            lastImage = map.filename
        }
        this.map = map
        projection = map.projection(realWidth.toFloat(), realHeight.toFloat())
        invalidate()
    }

    override fun onImageLoaded() {
        super.onImageLoaded()
        projection = map?.projection(realWidth.toFloat(), realHeight.toFloat())
        invalidate()
    }

    fun setMyLocation(coordinate: Coordinate?) {
        myLocation = coordinate
        invalidate()
    }

    private fun drawCalibrationPoints() {
        if (!showCalibrationPoints) return
        val calibrationPoints = map?.calibrationPoints ?: emptyList()
        for (i in calibrationPoints.indices) {
            val point = calibrationPoints[i]
            val sourceCoord = point.imageLocation.toPixels(
                realWidth.toFloat(),
                realHeight.toFloat()
            )
            val coord = sourceToViewCoord(sourceCoord.x, sourceCoord.y) ?: continue
            drawer.stroke(Color.WHITE)
            drawer.strokeWeight(drawer.dp(1f) / layerScale)
            drawer.fill(Color.BLACK)
            drawer.circle(coord.x, coord.y, drawer.dp(8f) / layerScale)

            drawer.textMode(TextMode.Center)
            drawer.fill(Color.WHITE)
            drawer.noStroke()
            drawer.textSize(drawer.dp(5f) / layerScale)
            drawer.text((i + 1).toString(), coord.x, coord.y)
        }
    }

    private fun drawMyLocation() {
        val scale = layerScale
        val location = myLocation ?: return
        val pixels = getPixelCoordinate(location) ?: return

        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(drawer.dp(1f) / scale)
        drawer.fill(AppColor.Orange.color)
        drawer.push()
        drawer.rotate(azimuth.value, pixels.x, pixels.y)
        drawer.triangle(
            pixels.x, pixels.y - drawer.dp(6f) / scale,
            pixels.x - drawer.dp(5f) / scale, pixels.y + drawer.dp(6f) / scale,
            pixels.x + drawer.dp(5f) / scale, pixels.y + drawer.dp(6f) / scale
        )
        drawer.pop()
    }

    fun showLocations(locations: List<IMappableLocation>) {
        this.locations = locations
        invalidate()
    }

    fun showPaths(paths: List<IMappablePath>) {
        this.paths = paths
        pathsRendered = false
        invalidate()
    }

    private fun generatePaths(paths: List<IMappablePath>): kotlin.collections.Map<Long, RenderedPath> {
        val metersPerPixel =
            map?.distancePerPixel(realWidth * scale, realHeight * scale)?.meters()?.distance ?: 1f
        val factory = RenderedPathFactory(metersPerPixel, null, 0f, true)
        val map = mutableMapOf<Long, RenderedPath>()
        for (path in paths) {
            val pathObj = pathPool.get()
            pathObj.reset()
            map[path.id] = factory.render(path.points.map { it.coordinate }, pathObj)
        }
        return map
    }

    private fun drawPaths() {
        val scale = layerScale / 1.5f
        if (!pathsRendered) {
            for (path in renderedPaths) {
                pathPool.release(path.value.path)
            }
            renderedPaths = generatePaths(paths)
            pathsRendered = true
        }

        val factory = PathLineDrawerFactory()
        drawer.push()
        drawer.clip(mapPath)
        for (path in paths) {
            val rendered = renderedPaths[path.id] ?: continue
            val lineDrawer = factory.create(path.style)
            val centerPixel = getPixelCoordinate(rendered.origin, false) ?: continue
            drawer.push()
            drawer.translate(centerPixel.x, centerPixel.y)
            lineDrawer.draw(drawer, path.color, strokeScale = scale) {
                path(rendered.path)
            }
            drawer.pop()
        }
        drawer.pop()
        drawer.noStroke()
        drawer.fill(Color.WHITE)
        drawer.noPathEffect()
    }

    fun highlightLocation(location: IMappableLocation?) {
        this.highlightedLocation = location
        invalidate()
    }

    private fun drawLocations() {
        val highlighted = highlightedLocation

        val gpsLocation = myLocation
        if (highlighted != null && gpsLocation != null) {
            val start = getPixelCoordinate(gpsLocation, false)
            val end = getPixelCoordinate(highlighted.coordinate, false)

            if (start != null && end != null) {
                drawer.noFill()
                drawer.stroke(highlighted.color)
                drawer.strokeWeight(drawer.dp(4f) / layerScale)
                drawer.opacity(127)
                drawer.line(start.x, start.y, end.x, end.y)
            }
        }

        var containsHighlighted = false
        for (location in locations) {
            if (location.id == highlighted?.id) {
                containsHighlighted = true
            }
            drawLocation(location, location.id == highlighted?.id || highlighted == null)
        }

        if (highlighted != null && !containsHighlighted) {
            drawLocation(highlighted, true)
        }

        drawer.opacity(255)
    }

    fun recenter() {
        resetScaleAndCenter()
    }

    fun showCalibrationPoints() {
        showCalibrationPoints = true
        invalidate()
    }

    fun hideCalibrationPoints() {
        showCalibrationPoints = false
        invalidate()
    }

    fun zoomBy(multiple: Float) {
        setScaleAndCenter((scale * multiple).coerceIn(minScale, maxScale), center)
    }

    private fun drawLocation(location: IMappableLocation, highlighted: Boolean) {
        val scale = layerScale
        val coord = getPixelCoordinate(location.coordinate)
        if (coord != null) {
            drawer.stroke(Color.WHITE)
            drawer.strokeWeight(drawer.dp(1f) / scale)
            drawer.fill(location.color)
            drawer.opacity(
                if (highlighted) {
                    255
                } else {
                    127
                }
            )
            drawer.circle(coord.x, coord.y, drawer.dp(8f) / scale)
        }
    }


    private fun getPixelCoordinate(
        coordinate: Coordinate,
        nullIfOffMap: Boolean = true
    ): PixelCoordinate? {

        val pixels = projection?.toPixels(coordinate) ?: return null

        if (nullIfOffMap && (pixels.x < 0 || pixels.x > sWidth)) {
            return null
        }

        if (nullIfOffMap && (pixels.y < 0 || pixels.y > sHeight)) {
            return null
        }

        val view = sourceToViewCoord(pixels.x, pixels.y)!!

        return PixelCoordinate(view.x, view.y)
    }


    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            val source = viewToSourceCoord(e.x, e.y) ?: return

            val coordinate = projection?.toCoordinate(Vector2(source.x, source.y))

            if (coordinate != null) {
                onMapLongClick?.invoke(coordinate)
            }
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {

            val clickRadius = drawer.dp(16f)

            val pixel = PixelCoordinate(e.x, e.y)

            for (location in locations) {
                val locationPixel = getPixelCoordinate(location.coordinate)
                if (locationPixel != null && locationPixel.distanceTo(pixel) < clickRadius) {
                    onLocationClick?.invoke(location)
                    break
                }
            }

            viewToSourceCoord(pixel.x, pixel.y)?.let {
                val percentX = it.x / realWidth
                val percentY = it.y / realHeight
                val percent = PercentCoordinate(percentX, percentY)
                onMapClick?.invoke(percent)
            }
            return super.onSingleTapConfirmed(e)
        }
    }

    private val realWidth: Int
        get() {
            return if (orientation == 90 || orientation == 270) {
                sHeight
            } else {
                sWidth
            }
        }

    private val realHeight: Int
        get() {
            return if (orientation == 90 || orientation == 270) {
                sWidth
            } else {
                sHeight
            }
        }

    private val gestureDetector = GestureDetector(context, gestureListener)


    override fun onTouchEvent(event: MotionEvent): Boolean {
        val consumed = gestureDetector.onTouchEvent(event)
        return consumed || super.onTouchEvent(event)
    }

}