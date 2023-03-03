package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.canvas.TextStyle
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geology.projections.IMapProjection
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.ui.DistanceScale
import com.kylecorry.trail_sense.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.navigation.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import kotlin.math.max
import kotlin.math.min


class PhotoMapView : SubsamplingScaleImageView, IMapView {

    var onMapLongClick: ((coordinate: Coordinate) -> Unit)? = null
    var onMapClick: ((percent: PercentCoordinate) -> Unit)? = null

    private lateinit var drawer: ICanvasDrawer
    private var isSetup = false

    private var map: PhotoMap? = null
    private val mapPath = Path()
    private var projection: IMapProjection? = null
    private val lookupMatrix = Matrix()

    private val prefs by lazy { UserPreferences(context) }
    private val units by lazy { prefs.baseDistanceUnits }
    private val formatService by lazy { FormatService(context) }
    private val scaleBar = Path()
    private val distanceScale = DistanceScale()

    private val layers = mutableListOf<ILayer>()

    private val files = FileSubsystem.getInstance(context)

    private var shouldRecenter = true

    private var lastTranslateX = 0f
    private var lastTranslateY = 0f

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
        return getPixelCoordinate(coordinate) ?: PixelCoordinate(0f, 0f)
    }

    override fun toCoordinate(pixel: PixelCoordinate): Coordinate {
        val source = toSource(pixel.x, pixel.y, true) ?: return Coordinate.zero
        return projection?.toCoordinate(Vector2(source.x, source.y)) ?: Coordinate.zero
    }

    private fun toPixel(point: PointF): PixelCoordinate {
        return PixelCoordinate(point.x, point.y)
    }

    override var metersPerPixel: Float
        get() = map?.distancePerPixel(realWidth * scale, realHeight * scale)?.meters()?.distance
            ?: 1f
        set(value) {
            requestScale(getScale(value))
        }

    private fun getScale(metersPerPixel: Float): Float {
        val fullScale =
            map?.distancePerPixel(realWidth.toFloat(), realHeight.toFloat())?.meters()?.distance
                ?: 1f
        return fullScale / metersPerPixel
    }

    override var mapCenter: Coordinate
        get() = toCoordinate(center?.let { toPixel(it) } ?: PixelCoordinate(
            width / 2f,
            height / 2f
        ))
        set(value) {
            val pixel = toPixel(value)
            requestCenter(toSource(pixel.x, pixel.y))
        }
    override var mapRotation: Float = 0f
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                refreshRequiredTiles(true)
                invalidate()
            }
        }

    var azimuth: Bearing = Bearing(0f)
        set(value) {
            field = value
            invalidate()
        }

    override val layerScale: Float
        get() = min(1f, max(scale, 0.9f))

    private var lastScale = 1f
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
        if (!isReady || canvas == null) {
            return
        }

        if (!isSetup) {
            drawer = CanvasDrawer(context, canvas)
            setup()
            isSetup = true
        }

        draw()
        // TODO: Use a flag instead
        tryOrNothing {
            drawer.pop()
        }

        drawScale()
        drawCompass()
    }

    fun setup() {
        setBackgroundColor(Resources.color(context, R.color.colorSecondary))
        setPanLimit(PAN_LIMIT_OUTSIDE)
        maxScale = 6f
        alwaysZoomDoubleTap = true
        alwaysZoomDoubleTapZoomScale = 2f
    }

    fun draw() {
        map ?: return

        // TODO: This only needs to be changed when the scale or translate changes
        mapPath.apply {
            rewind()
            val topLeft = toView(0f, 0f)!!
            val bottomRight = toView(realWidth.toFloat(), realHeight.toFloat())!!
            addRect(
                topLeft.x,
                topLeft.y,
                bottomRight.x,
                bottomRight.y,
                Path.Direction.CW
            )
        }

        drawer.push()
        drawer.clip(mapPath)
        if (scale != lastScale) {
            lastScale = scale
            layers.forEach { it.invalidate() }
        }

        vTranslate?.let {
            if (it.x != lastTranslateX || it.y != lastTranslateY) {
                lastTranslateX = it.x
                lastTranslateY = it.y
                layers.forEach { it.invalidate() }
            }
        }

        if (map?.calibration?.calibrationPoints?.size == 2) {
            maxScale = getScale(0.1f).coerceAtLeast(2 * minScale)
            if (shouldRecenter && isImageLoaded) {
                recenter()
            }
            layers.forEach { it.draw(drawer, this) }
        } else if (map != null) {
            // Don't recenter if the map is not calibrated
            shouldRecenter = false
        }
        drawer.pop()

        drawCalibrationPoints()
    }

    fun showMap(map: PhotoMap) {
        if (orientation != map.calibration.rotation) {
            orientation = when (map.calibration.rotation) {
                90 -> ORIENTATION_90
                180 -> ORIENTATION_180
                270 -> ORIENTATION_270
                else -> ORIENTATION_0
            }
        }
        if (lastImage != map.filename) {
            val uri = files.uri(map.filename)
            setImage(ImageSource.uri(uri))
            lastImage = map.filename
        }
        this.map = map
        projection = map.projection(realWidth.toFloat(), realHeight.toFloat())
        invalidate()
    }

    override fun tileVisible(tile: Tile?): Boolean {
        // No need to check if the map is not rotated
        if (mapRotation == 0f) {
            return super.tileVisible(tile)
        }

        val rect = tile?.sRect ?: return false

        // Calculate the bounds of the visible source area (factoring in rotation)
        val topLeftSource = toSource(0f, 0f, true)
        val topRightSource = toSource(width.toFloat(), 0f, true)
        val bottomRightSource = toSource(width.toFloat(), height.toFloat(), true)
        val bottomLeftSource = toSource(0f, height.toFloat(), true)

        val sVisLeft = minOf(
            topLeftSource?.x ?: 0f,
            topRightSource?.x ?: 0f,
            bottomRightSource?.x ?: 0f,
            bottomLeftSource?.x ?: 0f
        )
        val sVisRight = maxOf(
            topLeftSource?.x ?: 0f,
            topRightSource?.x ?: 0f,
            bottomRightSource?.x ?: 0f,
            bottomLeftSource?.x ?: 0f
        )
        val sVisTop = minOf(
            topLeftSource?.y ?: 0f,
            topRightSource?.y ?: 0f,
            bottomRightSource?.y ?: 0f,
            bottomLeftSource?.y ?: 0f
        )
        val sVisBottom = maxOf(
            topLeftSource?.y ?: 0f,
            topRightSource?.y ?: 0f,
            bottomRightSource?.y ?: 0f,
            bottomLeftSource?.y ?: 0f
        )

        // Check to see if the tile is within the visible source area
        return sVisLeft > rect.right || rect.left > sVisRight || sVisTop > rect.bottom || rect.top <= sVisBottom

    }

    override fun onImageLoaded() {
        super.onImageLoaded()
        projection = map?.projection(realWidth.toFloat(), realHeight.toFloat())
        shouldRecenter = true
        invalidate()
    }

    private fun drawCalibrationPoints() {
        if (!showCalibrationPoints) return
        val calibrationPoints = map?.calibration?.calibrationPoints ?: emptyList()
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
        shouldRecenter = false
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
        requestScale((scale * multiple).coerceIn(minScale, max(2 * minScale, maxScale)))
    }

    private fun getPixelCoordinate(coordinate: Coordinate): PixelCoordinate? {
        val pixels = projection?.toPixels(coordinate) ?: return null
        val view = toView(pixels.x, pixels.y)
        return PixelCoordinate(view?.x ?: 0f, view?.y ?: 0f)
    }

    private fun toView(sourceX: Float, sourceY: Float, withRotation: Boolean = false): PointF? {
        val view = sourceToViewCoord(sourceX, sourceY)
        if (!withRotation) {
            return view
        }
        val point = floatArrayOf(view?.x ?: 0f, view?.y ?: 0f)
        lookupMatrix.reset()
        lookupMatrix.postRotate(-mapRotation, width / 2f, height / 2f)
        lookupMatrix.mapPoints(point)
        view?.x = point[0]
        view?.y = point[1]
        return view
    }

    private fun toSource(viewX: Float, viewY: Float, withRotation: Boolean = false): PointF? {
        if (!withRotation) {
            return viewToSourceCoord(viewX, viewY)
        }
        val point = floatArrayOf(viewX, viewY)
        lookupMatrix.reset()
        lookupMatrix.postRotate(-mapRotation, width / 2f, height / 2f)
        lookupMatrix.invert(lookupMatrix)
        lookupMatrix.mapPoints(point)
        return viewToSourceCoord(point[0], point[1])
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)

            // Don't invoke if it is currently scaling
            if (isZooming || isQuickScaling) return

            val coordinate = toCoordinate(PixelCoordinate(e.x, e.y))
            onMapLongClick?.invoke(coordinate)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val pixel = toSource(e.x, e.y, true)
            val viewNoRotation = pixel?.let { toView(pixel.x, pixel.y, false) }

            // TODO: Pass in a coordinate rather than a pixel (convert radius to meters)
            if (viewNoRotation != null) {
                for (layer in layers.reversed()) {
                    val handled = layer.onClick(
                        drawer,
                        this@PhotoMapView,
                        PixelCoordinate(viewNoRotation.x, viewNoRotation.y)
                    )
                    if (handled) {
                        break
                    }
                }
            }

            pixel?.let {
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

    // TODO: Extract this (same way as scale)
    private fun drawCompass() {
        val compassSize = drawer.dp(36f)
        val arrowSize = drawer.dp(4f)
        val textSize = drawer.sp(8f)
        val text = context.getString(R.string.direction_north)
        val location = PixelCoordinate(
            width - drawer.dp(16f) - compassSize / 2f,
            drawer.dp(16f) + compassSize / 2f
        )
        drawer.push()
        drawer.rotate(-mapRotation, location.x, location.y)
        drawer.noTint()
        drawer.fill(Resources.color(context, R.color.colorSecondary))
        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(drawer.dp(1f))
        drawer.circle(location.x, location.y, drawer.dp(24f))

        drawer.fill(Color.WHITE)
        drawer.textMode(TextMode.Center)
        drawer.textSize(textSize)
        drawer.textStyle(TextStyle.Bold)
        drawer.noStroke()
        val textWidth = drawer.textWidth(text) // Not sure why this is needed to align the text
        drawer.text(text, location.x - textWidth / 8f, location.y)

        val arrowBtm = location.y - drawer.textHeight(text) / 2f - drawer.dp(2f)

        drawer.fill(AppColor.Orange.color)
        drawer.triangle(
            location.x - arrowSize / 2f,
            arrowBtm,
            location.x + arrowSize / 2f,
            arrowBtm,
            location.x,
            arrowBtm - arrowSize
        )

        drawer.textStyle(TextStyle.Normal)
        drawer.pop()
    }

    // TODO: Extract this to either a base mapview class, layer, or helper class
    private fun drawScale() {
        drawer.noFill()
        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(4f)

        val scaleSize = distanceScale.getScaleDistance(units, width / 2f, metersPerPixel)

        scaleBar.reset()
        distanceScale.getScaleBar(scaleSize, metersPerPixel, scaleBar)
        val start = width - drawer.dp(16f) - drawer.pathWidth(scaleBar)
        val y = height - drawer.dp(16f)
        drawer.push()
        drawer.translate(start, y)
        drawer.stroke(Color.BLACK)
        drawer.strokeWeight(8f)
        drawer.path(scaleBar)
        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(4f)
        drawer.path(scaleBar)
        drawer.pop()

        drawer.textMode(TextMode.Corner)
        drawer.textSize(drawer.sp(12f))
        drawer.strokeWeight(4f)
        drawer.stroke(Color.BLACK)
        drawer.fill(Color.WHITE)
        val scaleText =
            formatService.formatDistance(scaleSize, Units.getDecimalPlaces(scaleSize.units), false)
        drawer.text(
            scaleText,
            start - drawer.textWidth(scaleText) - drawer.dp(4f),
            y + drawer.textHeight(scaleText) / 2
        )
    }

}