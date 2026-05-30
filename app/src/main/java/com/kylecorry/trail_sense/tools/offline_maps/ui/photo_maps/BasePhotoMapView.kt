package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.map_layers.MapViewLayerManager
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toCoordinate
import com.kylecorry.trail_sense.shared.views.EnhancedImageView
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentCoordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections.PhotoMapProjection
import kotlin.math.max
import kotlin.math.min


abstract class BasePhotoMapView : EnhancedImageView, IMapView {

    private val density = context.resources.displayMetrics.density

    protected var map: PhotoMap? = null
    private var projection: IMapProjection? = null
    private var fullResolutionPixels = 1f
    override val layerManager = MapViewLayerManager {
        post { invalidate() }
    }
    private val files = FileSubsystem.getInstance(context)

    private var shouldRecenter = true
    private var isViewingPdf = false
    private var isViewingWarpedImageSource = false

    var onImageLoadedListener: (() -> Unit)? = null

    var useDensityPixelsForZoom = true

    private val hooks = Hooks()

    override var isWidget: Boolean = false

    override val isHighDetailMode: Boolean
        get() = !useDensityPixelsForZoom

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

    override val mapProjection: IMapViewProjection
        get() = hooks.memo(
            "mapProjection",
            projection,
            resolutionPixels,
            zoom,
            resolution,
        ) {
            val viewNoRotation = toViewNoRotation(center ?: PointF(width / 2f, height / 2f))
            val projection = projection
            val resolutionPixels = resolutionPixels
            val resolution = resolution
            val zoom = zoom

            object : IMapProjection, IMapViewProjection {
                override fun toPixels(
                    latitude: Double,
                    longitude: Double
                ): PixelCoordinate {
                    return getPixelCoordinate(latitude, longitude) ?: PixelCoordinate(0f, 0f)
                }

                override fun toCoordinate(pixel: Vector2): Coordinate {
                    // Convert to source - assume the view does not have the rotation offset applied. The resulting source will have the rotation offset applied.
                    val source = toSource(pixel.x, pixel.y) ?: return Coordinate.zero
                    val mapPixel = sourceToMapPixel(PixelCoordinate(source.x, source.y))
                    return projection?.toCoordinate(Vector2(mapPixel.x, mapPixel.y)) ?: Coordinate.zero
                }

                override fun toPixels(location: Coordinate): PixelCoordinate {
                    return getPixelCoordinate(location.latitude, location.longitude)
                        ?: PixelCoordinate(
                            0f,
                            0f
                        )
                }

                override val resolutionPixels: Float = resolutionPixels
                override val zoom: Float = zoom
                override val resolution: Float = resolution
                override val center: Coordinate by lazy {
                    viewNoRotation?.let {
                        toCoordinate(
                            toPixel(
                                it
                            )
                        )
                    } ?: Coordinate.zero
                }
            }
        }

    protected fun toPixel(point: PointF): PixelCoordinate {
        return PixelCoordinate(point.x, point.y)
    }

    override var resolutionPixels: Float
        get() = fullResolutionPixels / scale
        set(value) {
            requestScale(getScale(value))
        }

    override var resolution: Float
        get() = this@BasePhotoMapView.resolutionPixels * density
        set(value) {
            this@BasePhotoMapView.resolutionPixels = value / density
        }

    override val zoom: Float
        get() = TileMath.getZoomLevel(
            mapCenter,
            if (useDensityPixelsForZoom) resolution else resolutionPixels
        )

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

    private fun getScale(resolutionPixels: Float): Float {
        return fullResolutionPixels / resolutionPixels
    }

    override var mapCenter: Coordinate
        get() {
            val viewNoRotation = toViewNoRotation(center ?: PointF(width / 2f, height / 2f))
                ?: return Coordinate.zero
            val source = toSource(viewNoRotation.x, viewNoRotation.y) ?: return Coordinate.zero
            val mapPixel = sourceToMapPixel(PixelCoordinate(source.x, source.y))
            return projection?.toCoordinate(Vector2(mapPixel.x, mapPixel.y)) ?: Coordinate.zero
        }
        set(value) {
            val mapPixel = projection?.toPixels(value.latitude, value.longitude) ?: return
            val source = mapToSourcePixel(mapPixel)
            requestCenter(PointF(source.x, source.y))
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
        layerManager.invalidate()
    }

    override fun onTranslateChanged(
        oldTranslateX: Float,
        oldTranslateY: Float,
        newTranslateX: Float,
        newTranslateY: Float
    ) {
        super.onTranslateChanged(oldTranslateX, oldTranslateY, newTranslateX, newTranslateY)
        layerManager.invalidate()
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

        layerManager.draw(context, drawer, this)
    }

    override fun drawOverlay() {
        super.drawOverlay()
        layerManager.drawOverlay(context, drawer, this)
    }

    open fun showMap(map: PhotoMap) {
        this.map = map
        val rotation = map.calibration.rotation
        mapRotation = Trigonometry.deltaAngle(rotation, map.baseRotation().toFloat())
        fullResolutionPixels = map.distancePerPixel()?.meters()?.value ?: 1f
        val warpBounds = map.calibration.warpBounds
        isViewingWarpedImageSource = map.calibration.warped && warpBounds != null
        projection = if (isViewingWarpedImageSource) {
            PhotoMapProjection(map, usePdf = false)
        } else {
            map.baseProjection
        }
        if (keepMapUp) {
            mapAzimuth = 0f
        }

        isViewingPdf = files.get(map.pdfFileName).exists() && !isViewingWarpedImageSource
        if (isViewingWarpedImageSource && warpBounds != null) {
            val uri = WarpedPhotoMapImageSource.register(
                map.filename,
                map.metadata.imageSize,
                map.unrotatedSize(false),
                warpBounds
            )
            setImageSource(
                "warped:${map.id}:${map.filename}:${map.calibration.rotation}:${warpBounds.hashCode()}",
                uri,
                rotation,
                WarpedPhotoMapImageDecoder::class.java,
                WarpedPhotoMapImageRegionDecoder::class.java
            )
        } else if (isViewingPdf) {
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

    private fun getPixelCoordinate(latitude: Double, longitude: Double): PixelCoordinate? {
        val mapPixel = projection?.toPixels(latitude, longitude) ?: return null
        val source = mapToSourcePixel(mapPixel)
        val view = toView(source.x, source.y)
        return PixelCoordinate(view?.x ?: 0f, view?.y ?: 0f)
    }

    protected fun sourceToMapPixel(source: PixelCoordinate): PixelCoordinate {
        val unrotatedMapPixel = sourceToUnrotatedMapPixel(source)
        return rotateUnrotatedMapPixel(unrotatedMapPixel, orientation)
    }

    protected fun mapToSourcePixel(mapPixel: PixelCoordinate): PixelCoordinate {
        val unrotatedMapPixel = rotateMapPixelToUnrotated(mapPixel, -orientation)
        return unrotatedMapPixelToSource(unrotatedMapPixel)
    }

    protected fun sourceToUnrotatedMapPixel(source: PixelCoordinate): PixelCoordinate {
        val transform = getWarpTransform() ?: return rotateSourcePixelToUnrotated(source)
        val unrotatedSource = rotateSourcePixelToUnrotated(source)
        val points = floatArrayOf(unrotatedSource.x, unrotatedSource.y)
        transform.mapPoints(points)
        return PixelCoordinate(points[0], points[1])
    }

    protected fun unrotatedMapPixelToSource(unrotatedMapPixel: PixelCoordinate): PixelCoordinate {
        val transform = getWarpTransform() ?: return rotateUnrotatedSourcePixelToSource(unrotatedMapPixel)
        val inverse = Matrix()
        if (!transform.invert(inverse)) {
            return rotateUnrotatedSourcePixelToSource(unrotatedMapPixel)
        }
        val points = floatArrayOf(unrotatedMapPixel.x, unrotatedMapPixel.y)
        inverse.mapPoints(points)
        return rotateUnrotatedSourcePixelToSource(PixelCoordinate(points[0], points[1]))
    }

    private fun getWarpTransform(): Matrix? {
        val map = map ?: return null
        val warpBounds = map.calibration.warpBounds ?: return null
        if (!map.calibration.warped || isViewingWarpedImageSource || imageWidth == 0 || imageHeight == 0) {
            return null
        }
        val displayedSize = map.unrotatedSize(isViewingPdf)
        val sourceSize = unrotatedSourceSize()
        val bounds = warpBounds.toPixelBounds(sourceSize.width, sourceSize.height)
        return Matrix().apply {
            setPolyToPoly(
                floatArrayOf(
                    bounds.topLeft.x, bounds.topLeft.y,
                    bounds.topRight.x, bounds.topRight.y,
                    bounds.bottomRight.x, bounds.bottomRight.y,
                    bounds.bottomLeft.x, bounds.bottomLeft.y,
                ),
                0,
                floatArrayOf(
                    0f, 0f,
                    displayedSize.width, 0f,
                    displayedSize.width, displayedSize.height,
                    0f, displayedSize.height
                ),
                0,
                4
            )
        }
    }

    private fun rotateSourcePixelToUnrotated(source: PixelCoordinate): PixelCoordinate {
        val sourceSize = unrotatedSourceSize()
        return PercentCoordinate(source.x / imageWidth, source.y / imageHeight)
            .rotate(-orientation)
            .toPixels(sourceSize.width, sourceSize.height)
    }

    private fun rotateUnrotatedSourcePixelToSource(unrotatedSource: PixelCoordinate): PixelCoordinate {
        val sourceSize = unrotatedSourceSize()
        return PercentCoordinate(
            unrotatedSource.x / sourceSize.width,
            unrotatedSource.y / sourceSize.height
        ).rotate(orientation).toPixels(imageWidth, imageHeight)
    }

    private fun rotateUnrotatedMapPixel(unrotatedMapPixel: PixelCoordinate, rotation: Int): PixelCoordinate {
        val size = map?.unrotatedSize(isViewingPdf) ?: return unrotatedMapPixel
        val rotatedSize = size.rotate(rotation.toFloat())
        return PercentCoordinate(
            unrotatedMapPixel.x / size.width,
            unrotatedMapPixel.y / size.height
        ).rotate(rotation).toPixels(rotatedSize.width, rotatedSize.height)
    }

    private fun rotateMapPixelToUnrotated(mapPixel: PixelCoordinate, rotation: Int): PixelCoordinate {
        val size = map?.unrotatedSize(isViewingPdf) ?: return mapPixel
        val rotatedSize = size.rotate((-rotation).toFloat())
        return PercentCoordinate(
            mapPixel.x / rotatedSize.width,
            mapPixel.y / rotatedSize.height
        ).rotate(rotation).toPixels(size.width, size.height)
    }

    private fun unrotatedSourceSize(): com.kylecorry.sol.math.geometry.Size {
        return if (orientation == 90 || orientation == 270) {
            com.kylecorry.sol.math.geometry.Size(imageHeight.toFloat(), imageWidth.toFloat())
        } else {
            com.kylecorry.sol.math.geometry.Size(imageWidth.toFloat(), imageHeight.toFloat())
        }
    }

    /**
     * Convert from view with rotation to view without rotation
     */
    protected fun toViewNoRotation(view: PointF): PointF? {
        val source = toSource(view.x, view.y, true) ?: return null
        return toView(source.x, source.y, false)
    }

}
