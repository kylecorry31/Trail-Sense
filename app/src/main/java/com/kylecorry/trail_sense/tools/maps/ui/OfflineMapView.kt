package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geology.projections.IMapProjection
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.ui.IMappableLocation
import com.kylecorry.trail_sense.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.navigation.ui.layers.IMapView
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
    private val lookupMatrix = Matrix()

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
        get() = map?.distancePerPixel(realWidth * scale, realHeight * scale)?.meters()?.distance ?: 1f
        set(value) {
            requestScale(getScale(value))
        }

    private fun getScale(metersPerPixel: Float): Float {
        val fullScale = map?.distancePerPixel(realWidth.toFloat(), realHeight.toFloat())?.meters()?.distance ?: 1f
        return fullScale / metersPerPixel
    }

    override var centerLocation: Coordinate
        get() = toCoordinate(center?.let { toPixel(it) } ?: PixelCoordinate(
            width / 2f,
            height / 2f
        ))
        set(value) {
            requestCenter(viewToSourceCoord(toPoint(toPixel(value))))
        }
    override var mapRotation: Float = 0f
        set(value) {
            field = value
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
    private var pathsRendered = false
    private var lastScale = 1f
    private var lastRotation = 0f
    private var showCalibrationPoints = false

    private var lastImage: String? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onDraw(canvas: Canvas?) {

        if (isSetup && canvas != null) {
            drawer.canvas = canvas
            drawer.push()
            drawer.rotate(-mapRotation)
        }

        super.onDraw(canvas)
        // TODO: Use a flag instead
        tryOrNothing {
            drawer.pop()
        }
        if (!isReady || canvas == null) {
            return
        }

        if (!isSetup) {
            drawer = CanvasDrawer(context, canvas)
            setup()
            isSetup = true
        }

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
            val topLeft = toView(0f, 0f)!!
            val bottomRight = toView(realWidth.toFloat(), realHeight.toFloat())!!
            addRect(
                min(topLeft.x, bottomRight.x),
                max(topLeft.y, bottomRight.y),
                max(topLeft.x, bottomRight.x),
                min(topLeft.y, bottomRight.y),
                Path.Direction.CW)
        }

        drawer.push()
        drawer.clip(mapPath)
        if (scale != lastScale || mapRotation != lastRotation) {
            pathsRendered = false
            lastScale = scale
            lastRotation = mapRotation // TODO: Don't invalidate everytime the rotation changes
            layers.forEach { it.invalidate() }
        }

        drawCalibrationPoints()

        if (map?.calibrationPoints?.size == 2) {
            maxScale = getScale(0.1f)
            layers.forEach { it.draw(drawer, this) }
        }
        drawer.pop()
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
            val coord = toView(sourceCoord.x, sourceCoord.y) ?: continue
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
        requestScale((scale * multiple).coerceIn(minScale, maxScale))
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

        val view = toView(pixels.x, pixels.y)
        return PixelCoordinate(view?.x ?: 0f, view?.y ?: 0f)
    }

    private fun toView(sourceX: Float, sourceY: Float): PointF? {
        lookupMatrix.reset()
        val view = sourceToViewCoord(sourceX, sourceY)
        val point = floatArrayOf(view?.x ?: 0f, view?.y ?: 0f)
        lookupMatrix.postRotate(mapRotation, width / 2f, height / 2f)
        lookupMatrix.invert(lookupMatrix)
        lookupMatrix.mapPoints(point)
        view?.x = point[0]
        view?.y = point[1]
        return view
    }


    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            val coordinate = toCoordinate(PixelCoordinate(e.x, e.y))
            onMapLongClick?.invoke(coordinate)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {

//            val clickRadius = drawer.dp(16f)

            val pixel = PixelCoordinate(e.x, e.y)

            // TODO: Move tap functionality to beacon layer
            // onLayerClicked(x, y) -> Boolean (if handled), layer has a setClickListener
            // If handled, don't propagate to nextlayer
//            for (location in locations) {
//                val locationPixel = getPixelCoordinate(location.coordinate)
//                if (locationPixel != null && locationPixel.distanceTo(pixel) < clickRadius) {
//                    onLocationClick?.invoke(location)
//                    break
//                }
//            }

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