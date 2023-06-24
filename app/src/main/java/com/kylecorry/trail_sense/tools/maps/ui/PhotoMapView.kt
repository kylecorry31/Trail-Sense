package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
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
import com.kylecorry.trail_sense.shared.views.EnhancedImageView
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import kotlin.math.max
import kotlin.math.min


class PhotoMapView : EnhancedImageView, IMapView {

    var onMapLongClick: ((coordinate: Coordinate) -> Unit)? = null

    private var map: PhotoMap? = null
    private var projection: IMapProjection? = null
    private var fullMetersPerPixel = 1f

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
        // Convert to source - assume the view does not have the rotation offset applied. The resulting source will have the rotation offset applied.
        val source = toSource(pixel.x, pixel.y) ?: return Coordinate.zero
        return projection?.toCoordinate(Vector2(source.x, source.y)) ?: Coordinate.zero
    }

    private fun toPixel(point: PointF): PixelCoordinate {
        return PixelCoordinate(point.x, point.y)
    }

    override var metersPerPixel: Float
        get() = fullMetersPerPixel / scale
        set(value) {
            requestScale(getScale(value))
        }

    private fun getScale(metersPerPixel: Float): Float {
        return fullMetersPerPixel / metersPerPixel
    }

    override var mapCenter: Coordinate
        get(){
            val viewNoRotation = toViewNoRotation(center ?: PointF(width / 2f, height / 2f)) ?: return Coordinate.zero
            return toCoordinate(toPixel(viewNoRotation))
        }
        set(value) {
            val pixel = toPixel(value)
            requestCenter(toSource(pixel.x, pixel.y))
        }
    override var mapAzimuth: Float = 0f
        set(value) {
            val newValue = if (keepMapUp) {
                -mapRotation
            } else {
                value
            }
            val changed = field != newValue
            field = newValue
            if (changed) {
                imageRotation = newValue
                invalidate()
            }
        }

    /**
     * Determines if the map should be kept with its top parallel to the top of the screen
     */
    var keepMapUp: Boolean = false
        set(value) {
            field = value
            if (value) {
                mapAzimuth = 0f
            }
            invalidate()
        }

    override var mapRotation: Float = 0f
        private set(value){
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

        if (!map.isCalibrated) {
            return
        }
        maxScale = getScale(0.1f).coerceAtLeast(2 * minScale)
        if (shouldRecenter && isImageLoaded) {
            recenter()
            shouldRecenter = false
        }

        layers.forEach { it.draw(drawer, this) }
    }

    fun showMap(map: PhotoMap) {
        this.map = map
        // TODO: When not rotateNorthUp, my location / map doesn't properly rotate to face north
        val rotation = map.calibration.rotation
        mapRotation = SolMath.deltaAngle(rotation, map.baseRotation().toFloat())
        fullMetersPerPixel = map.distancePerPixel()?.meters()?.distance ?: 1f
        projection = map.projection
        if (keepMapUp){
            mapAzimuth = 0f
        }
        setImage(map.filename, rotation)
    }

    override fun onImageLoaded() {
        super.onImageLoaded()
        shouldRecenter = true
        invalidate()
    }

    private fun getPixelCoordinate(coordinate: Coordinate): PixelCoordinate? {
        val source = projection?.toPixels(coordinate) ?: return null
        val view = toView(source.x, source.y)
        return PixelCoordinate(view?.x ?: 0f, view?.y ?: 0f)
    }

    override fun onLongPress(e: MotionEvent) {
        super.onLongPress(e)
        val viewNoRotation = toViewNoRotation(PointF(e.x, e.y)) ?: return
        val coordinate = toCoordinate(toPixel(viewNoRotation))
        onMapLongClick?.invoke(coordinate)
    }

    /**
     * Convert from view with rotation to view without rotation
     */
    private fun toViewNoRotation(view: PointF): PointF? {
        val source = toSource(view.x, view.y, true) ?: return null
        return toView(source.x, source.y, false)
    }

    override fun onSinglePress(e: MotionEvent) {
        super.onSinglePress(e)
        val viewNoRotation = toViewNoRotation(PointF(e.x, e.y))

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
    }

    // TODO: Extract this (same way as scale)
    private fun drawCompass() {
        val compassSize = drawer.dp(24f)
        val arrowWidth = drawer.dp(5f)
        val arrowMargin = drawer.dp(3f)
        val location = PixelCoordinate(
            width - drawer.dp(32f),
            drawer.dp(32f)
        )
        drawer.push()
        drawer.rotate(-mapAzimuth, location.x, location.y)

        // Background circle
        drawer.noTint()
        drawer.fill(Resources.color(context, R.color.colorSecondary))
        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(drawer.dp(1f))
        drawer.circle(location.x, location.y, compassSize)

        // Top triangle
        drawer.noStroke()
        drawer.fill(AppColor.Orange.color)
        drawer.triangle(
            location.x,
            location.y - compassSize / 2f + arrowMargin,
            location.x - arrowWidth / 2f,
            location.y,
            location.x + arrowWidth / 2f,
            location.y
        )

        // Bottom triangle
        drawer.fill(Color.WHITE)
        drawer.triangle(
            location.x,
            location.y + compassSize / 2f - arrowMargin,
            location.x - arrowWidth / 2f,
            location.y,
            location.x + arrowWidth / 2f,
            location.y
        )

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