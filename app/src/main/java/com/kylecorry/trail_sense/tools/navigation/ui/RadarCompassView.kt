package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import com.kylecorry.andromeda.canvas.ArcMode
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.canvas.TextStyle
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.geometry.Circle
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.map_layers.MapViewLayerManager
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import kotlin.math.min

class RadarCompassView : BaseCompassView, IMapView {
    private val density = context.resources.displayMetrics.density
    private var centerPixel: PixelCoordinate = PixelCoordinate(0f, 0f)
    private lateinit var compassCircle: Circle
    var useDensityPixelsForZoom = true

    @ColorInt
    private var primaryColor: Int = Color.WHITE

    @ColorInt
    private var secondaryColor: Int = Color.WHITE

    @ColorInt
    private var textColor: Int = Color.WHITE

    private val formatService by lazy { FormatService.getInstance(context) }

    private var iconSize = 0
    private var radarSize = 0
    private var directionSize = 0
    private var compassSize = 0
    private lateinit var compassPath: Path
    private var distanceSize = 0f
    private var cardinalSize = 0f
    private var locationStrokeWeight = 0f

    private var maxDistanceBaseUnits: Distance = Distance.meters(0f)
    private var maxDistanceMeters: Distance = Distance.meters(0f)

    private val navigation = NavigationService()

    override val layerManager = MapViewLayerManager {
        post { invalidate() }
    }

    private var singleTapAction: (() -> Unit)? = null
    private var longPressAction: (() -> Unit)? = null

    private var isScaling = false

    private var north = ""
    private var south = ""
    private var east = ""
    private var west = ""

    private var distanceText: String? = null

    private lateinit var dial: CompassDial
    private var lastWidth = 0
    private var lastHeight = 0

    var shouldDrawDial: Boolean = true
    var shouldDrawAzimuthIndicator: Boolean = true

    private val hooks = Hooks()

    override var userLocation: Coordinate
        get() = compassCenter
        set(value) {
            compassCenter = value
        }

    override var userLocationAccuracy: Distance? = null
        set(value) {
            field = value
            invalidate()
        }

    override var userAzimuth: Bearing
        get() = Bearing.from(azimuth)
        set(value) {
            azimuth = value.value
        }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setOnSingleTapListener(action: (() -> Unit)?) {
        singleTapAction = action
    }

    fun setOnLongPressListener(action: (() -> Unit)?) {
        longPressAction = action
    }

    private fun drawAzimuth() {
        tint(Resources.androidTextColorPrimary(context))
        imageMode(ImageMode.Corner)
        image(
            getBitmap(R.drawable.ic_arrow_target, iconSize),
            width / 2f - iconSize / 2f,
            0f
        )
        noTint()
    }

    private fun drawLayers() {
        // TODO: Handle beacon highlighting
        push()
        clip(compassPath)
        layerManager.draw(context, this, this)
        pop()
    }

    private fun drawOverlays() {
        layerManager.drawOverlay(context, this, this)
    }

    private fun drawCompass() {
        imageMode(ImageMode.Center)

        dial.draw(drawer, shouldDrawDial, false)

        noFill()
        stroke(Color.WHITE)
        opacity(30)
        strokeWeight(3f)
        push()
        rotate(azimuth)
        if (shouldDrawDial) {
            line(width / 2f, height / 2f, width / 2f, iconSize + dp(2f))
        }
        circle(width / 2f, height / 2f, compassSize / 2f)
        circle(width / 2f, height / 2f, 3 * compassSize / 4f)
        circle(width / 2f, height / 2f, compassSize / 4f)

        // Distance Text
        if (distanceText == null) {
            val distance = maxDistanceBaseUnits.toRelativeDistance()
            distanceText = formatService.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
            )
        }

        textSize(distanceSize)
        fill(textColor)
        noStroke()
        textMode(TextMode.Corner)
        opacity(200)
        distanceText?.let {
            text(
                it,
                (width - compassSize) / 2f + 16,
                height - (height - compassSize) / 2f + 16
            )
        }
        pop()


        // Directions
        if (shouldDrawDial) {
            drawCardinalDirections()
        }

        imageMode(ImageMode.Corner)
    }

    private fun drawCardinalDirections() {
        textMode(TextMode.Center)
        textSize(cardinalSize)
        stroke(secondaryColor)
        textStyle(TextStyle.Bold)
        strokeWeight(dp(1f))
        opacity(255)
        drawDirection(0f, north)
        drawDirection(90f, east)
        drawDirection(180f, south)
        drawDirection(270f, west)
    }

    private fun drawDirection(degrees: Float, text: String) {
        push()
        rotate(degrees)
        fill(primaryColor)
        text(
            text,
            width / 2f,
            height / 2f - (compassSize / 2f) * 0.9f
        )
        pop()
    }

    override fun setup() {
        super.setup()
        iconSize = dp(24f).toInt()
        radarSize = dp(10f).toInt()
        directionSize = dp(16f).toInt()
        compassSize = min(height, width) - 2 * iconSize - 2 * Resources.dp(context, 2f).toInt()
        compassPath = Path().apply {
            addCircle(width / 2f, height / 2f, compassSize / 2f, Path.Direction.CW)
        }
        distanceSize = sp(10f)
        cardinalSize = sp(20f)
        primaryColor = Resources.getCardinalDirectionColor(context)
        secondaryColor = Resources.color(context, R.color.colorSecondary)
        textColor = Resources.androidTextColorSecondary(context)
        maxDistanceMeters = Distance.meters(prefs.navigation.radarViewDistance)
        maxDistanceBaseUnits = maxDistanceMeters.convertTo(prefs.baseDistanceUnits)
        distanceText = null
        north = context.getString(R.string.direction_north)
        south = context.getString(R.string.direction_south)
        east = context.getString(R.string.direction_east)
        west = context.getString(R.string.direction_west)
        centerPixel = PixelCoordinate(width / 2f, height / 2f)
        compassCircle = Circle(Vector2(centerPixel.x, centerPixel.y), compassSize / 2f)
        locationStrokeWeight = dp(0.5f)
        dial = CompassDial(
            centerPixel,
            compassSize / 2f,
            secondaryColor,
            Color.WHITE,
            primaryColor,
            hideTrueCardinalTicks = true
        )
        lastWidth = width
        lastHeight = height
    }

    override fun draw() {
        if (!isVisible) {
            return
        }
        if (lastWidth != width || lastHeight != height) {
            setup()
        }
        clear()
        if (shouldDrawAzimuthIndicator) {
            drawAzimuth()
        }
        push()
        rotate(-azimuth)
        dial.draw(drawer, false)
        drawLayers()
        drawCompassLayers()
        drawCompass()
        pop()
        drawOverlays()
    }

    override fun draw(reference: IMappableReferencePoint, size: Int?) {
        val sizeDp = size?.let { dp(it.toFloat()).toInt() } ?: iconSize

        if (reference.opacity == 0f) {
            return
        }
        val tint = reference.tint
        if (tint != null) {
            tint(tint)
        } else {
            noTint()
        }
        opacity((255 * reference.opacity).toInt())
        push()
        rotate(reference.bearing)
        val bitmap = getBitmap(reference.drawableId, sizeDp)
        push()
        translate(width / 2f - sizeDp / 2f, (iconSize - sizeDp) * 0.6f)
        rotate(reference.rotation, bitmap.width / 2f, bitmap.height / 2f)
        image(bitmap, 0f, 0f)
        pop()
        pop()
        noTint()
        opacity(255)
        noStroke()
    }

    override fun draw(bearing: IMappableBearing, stopAt: Coordinate?) {
        push()
        fill(bearing.color)

        // To end of compass
        opacity(if (stopAt != null) 25 else 100)
        val dp2 = dp(2f)
        arc(
            iconSize.toFloat() + dp2,
            iconSize.toFloat() + dp2,
            compassSize.toFloat(),
            compassSize.toFloat(),
            azimuth - 90,
            azimuth - 90 + deltaAngle(azimuth, bearing.bearing),
            ArcMode.Pie
        )

        // To highlighted location
        stopAt?.let {
            val pixel = toPixel(it)
            val size = min(compassSize.toFloat(), pixel.distanceTo(centerPixel) * 2)
            opacity(75)
            arc(
                centerPixel.x - size / 2f,
                centerPixel.y - size / 2f,
                size,
                size,
                azimuth - 90,
                azimuth - 90 + deltaAngle(azimuth, bearing.bearing),
                ArcMode.Pie
            )
        }

        opacity(255)
        pop()
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            isScaling = false
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            prefs.navigation.radarViewDistance /= detector.scaleFactor
            maxDistanceMeters = Distance.meters(prefs.navigation.radarViewDistance)
            maxDistanceBaseUnits = maxDistanceMeters.convertTo(prefs.baseDistanceUnits)
            distanceText = null
            layerManager.invalidate()
            return true
        }
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            singleTapAction?.invoke()
            return super.onSingleTapConfirmed(e)
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            if (!isScaling) {
                longPressAction?.invoke()
            }
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)
    private val mGestureDetector = GestureDetector(context, gestureListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)
        mGestureDetector.onTouchEvent(event)
        invalidate()
        return true
    }

    override val mapProjection: IMapViewProjection
        get() = hooks.memo(
            "mapProjection",
            mapCenter,
            resolutionPixels,
            resolution,
            zoom
        ) {
            val mapCenter = mapCenter
            val resolutionPixels = resolutionPixels
            val resolution = resolution
            val zoom = zoom

            object : IMapProjection, IMapViewProjection {
                override fun toPixels(
                    latitude: Double,
                    longitude: Double
                ): PixelCoordinate {
                    return toPixels(Coordinate(latitude, longitude))
                }

                override fun toCoordinate(pixel: Vector2): Coordinate {
                    // Not implemented
                    return mapCenter
                }

                override fun toPixels(location: Coordinate): PixelCoordinate {
                    val vector =
                        navigation.navigate(compassCenter, location, declination, useTrueNorth)
                    val angle = Arithmetic.wrap(-(vector.direction.value - 90), 0f, 360f)
                    val pixelDistance = vector.distance / resolutionPixels
                    val xDiff = Trigonometry.cosDegrees(angle) * pixelDistance
                    val yDiff = Trigonometry.sinDegrees(angle) * pixelDistance
                    return PixelCoordinate(
                        compassCircle.center.x + xDiff,
                        compassCircle.center.y - yDiff
                    )
                }

                override val resolutionPixels: Float = resolutionPixels
                override val resolution: Float = resolution
                override val zoom: Float = zoom
                override val center: Coordinate = mapCenter
            }
        }

    override var resolutionPixels: Float
        get() = maxDistanceMeters.value / (compassSize / 2f)
        set(_) {
            // Do nothing yet
        }

    override var resolution: Float
        get() = this@RadarCompassView.resolutionPixels * density
        set(value) {
            this@RadarCompassView.resolutionPixels = value / density
        }

    override val zoom: Float
        get() = TileMath.getZoomLevel(
            mapCenter,
            if (useDensityPixelsForZoom) resolution else resolutionPixels
        )

    override val layerScale: Float = 1f

    override var mapCenter: Coordinate
        get() = compassCenter
        set(value) {
            compassCenter = value
        }
    override var mapAzimuth: Float
        get() = azimuth
        set(value) {
            azimuth = value
        }

    override val mapRotation: Float = 0f

    override val mapBounds: CoordinateBounds
        get() {
            val geofence = Geofence(
                mapCenter,
                maxDistanceBaseUnits
            )
            return CoordinateBounds.from(geofence)
        }

    override fun setOnGeoJsonFeatureClickListener(listener: ((com.kylecorry.andromeda.geojson.GeoJsonFeature) -> Unit)?) {
        // Not supported
    }
}
