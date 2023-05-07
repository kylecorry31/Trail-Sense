package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.canvas.TextStyle
import com.kylecorry.andromeda.core.system.Resources
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
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import kotlin.math.max
import kotlin.math.min


class PhotoMapView : EnhancedImageView, IMapView {

    var onMapLongClick: ((coordinate: Coordinate) -> Unit)? = null
    var onMapClick: ((percent: PercentCoordinate) -> Unit)? = null

    private var map: PhotoMap? = null
    private var projection: IMapProjection? = null

    private val prefs by lazy { UserPreferences(context) }
    private val units by lazy { prefs.baseDistanceUnits }
    private val formatService by lazy { FormatService.getInstance(context) }
    private val scaleBar = Path()
    private val distanceScale = DistanceScale()

    private val layers = mutableListOf<ILayer>()

    private var shouldRecenter = true

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
        get() = map?.distancePerPixel(imageWidth * scale, imageHeight * scale)?.meters()?.distance
            ?: 1f
        set(value) {
            requestScale(getScale(value))
        }

    private fun getScale(metersPerPixel: Float): Float {
        val fullScale =
            map?.distancePerPixel(imageWidth.toFloat(), imageHeight.toFloat())?.meters()?.distance
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
                imageRotation = value
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

    private var showCalibrationPoints = false

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun drawOverlay() {
        super.drawOverlay()
        drawScale()
        drawCompass()
    }

    override fun setup() {
        super.setup()
        setBackgroundColor(Resources.color(context, R.color.colorSecondary))
    }

    override fun onScaleChanged(oldScale: Float, newScale: Float) {
        super.onScaleChanged(oldScale, newScale)
        layers.forEach { it.invalidate() }
    }

    override fun onTranslateChanged(
        oldTranslateX: Float,
        oldTranslateY: Float,
        newTranslateX: Float,
        newTranslateY: Float
    ) {
        super.onTranslateChanged(oldTranslateX, oldTranslateY, newTranslateX, newTranslateY)
        layers.forEach { it.invalidate() }
    }

    override fun draw() {
        super.draw()
        val map = map ?: return

//        if (!map.isCalibrated) {
//            return
//        }
//        maxScale = getScale(0.1f).coerceAtLeast(2 * minScale)
//        if (shouldRecenter && isImageLoaded) {
//            recenter()
//            shouldRecenter = false
//        }

        val isCalibrating = showCalibrationPoints
        val hasCalibrationPoints = map.isCalibrated
        val hasActualCalibration =
            map.calibration.calibrationPoints.all { it.location != Coordinate.zero }
        if (hasCalibrationPoints && (!isCalibrating || hasActualCalibration)) {
            maxScale = getScale(0.1f).coerceAtLeast(2 * minScale)
            if (shouldRecenter && isImageLoaded) {
                recenter()
                shouldRecenter = false
            }
            layers.forEach { it.draw(drawer, this) }
        } else {
            // Don't recenter if the map is not calibrated
            shouldRecenter = false
        }

        layers.forEach { it.draw(drawer, this) }
    }

    override fun postDraw() {
        super.postDraw()
        drawCalibrationPoints()
    }


    fun showMap(map: PhotoMap) {
        this.map = map
        projection = map.projection(imageWidth.toFloat(), imageHeight.toFloat())
        setImage(map.filename, map.calibration.rotation)
    }

    override fun onImageLoaded() {
        super.onImageLoaded()
        projection = map?.projection(imageWidth.toFloat(), imageHeight.toFloat())
        shouldRecenter = true
        invalidate()
    }

    private fun getPixelCoordinate(coordinate: Coordinate): PixelCoordinate? {
        val pixels = projection?.toPixels(coordinate) ?: return null
        val view = toView(pixels.x, pixels.y)
        return PixelCoordinate(view?.x ?: 0f, view?.y ?: 0f)
    }

    override fun onLongPress(e: MotionEvent) {
        super.onLongPress(e)
        val coordinate = toCoordinate(PixelCoordinate(e.x, e.y))
        onMapLongClick?.invoke(coordinate)
    }

    override fun onSinglePress(e: MotionEvent) {
        super.onSinglePress(e)
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
            val percentX = it.x / imageWidth
            val percentY = it.y / imageHeight
            val percent = PercentCoordinate(percentX, percentY)
            onMapClick?.invoke(percent)
        }
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

    fun showCalibrationPoints() {
        showCalibrationPoints = true
        invalidate()
    }

    fun hideCalibrationPoints() {
        showCalibrationPoints = false
        invalidate()
    }

    private fun drawCalibrationPoints() {
        if (!showCalibrationPoints) return
        val calibrationPoints = map?.calibration?.calibrationPoints ?: emptyList()
        for (i in calibrationPoints.indices) {
            val point = calibrationPoints[i]
            val sourceCoord = point.imageLocation.toPixels(
                imageWidth.toFloat(),
                imageHeight.toFloat()
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

}