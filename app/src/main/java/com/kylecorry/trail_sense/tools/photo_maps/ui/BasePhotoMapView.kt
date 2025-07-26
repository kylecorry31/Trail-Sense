package com.kylecorry.trail_sense.tools.photo_maps.ui

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.views.EnhancedImageView
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import kotlin.math.max
import kotlin.math.min


abstract class BasePhotoMapView : EnhancedImageView, IMapView {

    protected var map: PhotoMap? = null
    private var projection: IMapProjection? = null
    private var fullMetersPerPixel = 1f
    protected val layers = mutableListOf<ILayer>()
    private val files = FileSubsystem.getInstance(context)

    private var shouldRecenter = true

    var onImageLoadedListener: (() -> Unit)? = null

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

    protected fun toPixel(point: PointF): PixelCoordinate {
        return PixelCoordinate(point.x, point.y)
    }

    override var metersPerPixel: Float
        get() = fullMetersPerPixel / scale
        set(value) {
            requestScale(getScale(value))
        }

    override val mapBounds: CoordinateBounds
        get() {
            val topLeft = toCoordinate(
                PixelCoordinate(0f, 0f)
            )
            val topRight = toCoordinate(
                PixelCoordinate(width.toFloat(), 0f)
            )
            val bottomRight = toCoordinate(
                PixelCoordinate(width.toFloat(), height.toFloat())
            )
            val bottomLeft = toCoordinate(
                PixelCoordinate(0f, height.toFloat())
            )
            return CoordinateBounds.from(
                listOf(
                    topLeft,
                    topRight,
                    bottomRight,
                    bottomLeft
                )
            )
        }

    private fun getScale(metersPerPixel: Float): Float {
        return fullMetersPerPixel / metersPerPixel
    }

    override var mapCenter: Coordinate
        get() {
            val viewNoRotation = toViewNoRotation(center ?: PointF(width / 2f, height / 2f))
                ?: return Coordinate.zero
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
        protected set(value) {
            field = value
            invalidate()
        }

    var azimuth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    override val layerScale: Float
        get() = min(1f, max(scale, 0.9f))

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

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
            onImageLoadedListener?.invoke()
        }

        layers.forEach { it.draw(drawer, this) }
    }

    override fun drawOverlay() {
        super.drawOverlay()
        layers.forEach { it.drawOverlay(drawer, this) }
    }

    open fun showMap(map: PhotoMap) {
        this.map = map
        val rotation = map.calibration.rotation
        mapRotation = SolMath.deltaAngle(rotation, map.baseRotation().toFloat())
        fullMetersPerPixel = map.distancePerPixel()?.meters()?.distance ?: 1f
        projection = map.baseProjection
        if (keepMapUp) {
            mapAzimuth = 0f
        }

        if (files.get(map.pdfFileName).exists()) {
            setImage(map.pdfFileName, rotation)
        } else {
            setImage(map.filename, rotation)
        }
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

    /**
     * Convert from view with rotation to view without rotation
     */
    protected fun toViewNoRotation(view: PointF): PointF? {
        val source = toSource(view.x, view.y, true) ?: return null
        return toView(source.x, source.y, false)
    }

}