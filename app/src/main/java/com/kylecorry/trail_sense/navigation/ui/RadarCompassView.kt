package com.kylecorry.trail_sense.navigation.ui

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
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Circle
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.RadarCompassCoordinateToPixelStrategy
import com.kylecorry.trail_sense.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.navigation.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.maps.ICoordinateToPixelStrategy
import kotlin.math.min

class RadarCompassView : BaseCompassView, IMapView {
    private lateinit var centerPixel: PixelCoordinate

    @ColorInt
    private var primaryColor: Int = Color.WHITE

    @ColorInt
    private var secondaryColor: Int = Color.WHITE

    @ColorInt
    private var textColor: Int = Color.WHITE

    private val formatService by lazy { FormatService(context) }

    private var iconSize = 0
    private var radarSize = 0
    private var directionSize = 0
    private var compassSize = 0
    private lateinit var compassPath: Path
    private var distanceSize = 0f
    private var cardinalSize = 0f
    private var locationStrokeWeight = 0f

    private lateinit var maxDistanceBaseUnits: Distance
    private lateinit var maxDistanceMeters: Distance
    private val coordinateToPixelStrategy: ICoordinateToPixelStrategy
        get() {
            return RadarCompassCoordinateToPixelStrategy(
                Circle(Vector2(centerPixel.x, centerPixel.y), compassSize / 2f),
                Geofence(_location, maxDistanceMeters),
                _useTrueNorth,
                _declination
            )
        }

    private val layers = mutableListOf<ILayer>()

    private var singleTapAction: (() -> Unit)? = null

    private var north = ""
    private var south = ""
    private var east = ""
    private var west = ""

    private lateinit var dial: CompassDial

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

    private fun drawDestination() {
        val destination = _destination ?: return
        val color = destination.color
        push()
        fill(color)

        // To end of compass
        opacity(if (_highlightedLocation != null) 25 else 100)
        val dp2 = dp(2f)
        arc(
            iconSize.toFloat() + dp2,
            iconSize.toFloat() + dp2,
            compassSize.toFloat(),
            compassSize.toFloat(),
            azimuth.value - 90,
            azimuth.value - 90 + deltaAngle(azimuth.value, destination.bearing.value),
            ArcMode.Pie
        )

        // To highlighted location
        _highlightedLocation?.let {
            val pixel = toPixel(it.coordinate)
            val size = min(compassSize.toFloat(), pixel.distanceTo(centerPixel) * 2)
            opacity(75)
            arc(
                centerPixel.x - size / 2f,
                centerPixel.y - size / 2f,
                size,
                size,
                azimuth.value - 90,
                azimuth.value - 90 + deltaAngle(azimuth.value, destination.bearing.value),
                ArcMode.Pie
            )
        }


        opacity(255)
        pop()
        drawReferencePoint(
            MappableReferencePoint(
                0,
                R.drawable.ic_arrow_target,
                destination.bearing,
                destination.color
            )
        )

        _highlightedLocation?.let { location ->
            location.icon?.let { icon ->
                drawReferencePoint(
                    MappableReferencePoint(
                        location.id,
                        icon.icon,
                        destination.bearing,
                        Colors.mostContrastingColor(Color.WHITE, Color.BLACK, destination.color)
                    ),
                    (iconSize * 0.35f).toInt()
                )
            }
        }
    }

    private fun drawLayers() {
        // TODO: Handle beacon highlighting
        push()
        clip(compassPath)
        layers.forEach { it.draw(this, this) }
        pop()
    }

    private fun drawReferencePoints() {
        _references.forEach { drawReferencePoint(it) }
    }

    private fun drawReferencePoint(reference: IMappableReferencePoint, size: Int = iconSize) {
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
        rotate(reference.bearing.value)
        val bitmap = getBitmap(reference.drawableId, size)
        image(bitmap, width / 2f - size / 2f, (iconSize - size) * 0.6f)
        pop()
        noTint()
        opacity(255)
        noStroke()
    }

    private fun drawCompass() {
        imageMode(ImageMode.Center)

        dial.draw(drawer)

        noFill()
        stroke(Color.WHITE)
        opacity(30)
        strokeWeight(3f)
        push()
        rotate(azimuth.value)
        if (_destination == null) {
            line(width / 2f, height / 2f, width / 2f, iconSize + dp(2f))
        }
        circle(width / 2f, height / 2f, compassSize / 2f)
        circle(width / 2f, height / 2f, 3 * compassSize / 4f)
        circle(width / 2f, height / 2f, compassSize / 4f)

        // Distance Text
        val distance = maxDistanceBaseUnits.toRelativeDistance()
        val distanceText = formatService.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )

        textSize(distanceSize)
        fill(textColor)
        noStroke()
        textMode(TextMode.Corner)
        opacity(200)
        text(
            distanceText,
            (width - compassSize) / 2f + 16,
            height - (height - compassSize) / 2f + 16
        )

        // Directions
        pop()
        textMode(TextMode.Center)
        textSize(cardinalSize)
        stroke(Resources.color(context, R.color.colorSecondary))
        opacity(255)
        push()
        rotate(0f)
        fill(Color.WHITE)
        text(
            north,
            width / 2f,
            height / 2f - compassSize / 4f
        )
        pop()

        push()
        rotate(180f)
        fill(Color.WHITE)
        text(
            south,
            width / 2f,
            height / 2f - compassSize / 4f
        )
        pop()

        push()
        rotate(90f)
        fill(Color.WHITE)
        text(
            east,
            width / 2f,
            height / 2f - compassSize / 4f
        )
        pop()

        push()
        rotate(270f)
        fill(Color.WHITE)
        text(
            west,
            width / 2f,
            height / 2f - compassSize / 4f
        )
        pop()

        imageMode(ImageMode.Corner)
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
        cardinalSize = sp(12f)
        primaryColor = Resources.color(context, R.color.orange_40)
        secondaryColor = Resources.color(context, R.color.colorSecondary)
        textColor = Resources.androidTextColorSecondary(context)
        maxDistanceMeters = Distance.meters(prefs.navigation.maxBeaconDistance)
        maxDistanceBaseUnits = maxDistanceMeters.convertTo(prefs.baseDistanceUnits)
        north = context.getString(R.string.direction_north)
        south = context.getString(R.string.direction_south)
        east = context.getString(R.string.direction_east)
        west = context.getString(R.string.direction_west)
        centerPixel = PixelCoordinate(width / 2f, height / 2f)
        locationStrokeWeight = dp(0.5f)
        dial = CompassDial(centerPixel, compassSize / 2f, secondaryColor, Color.WHITE, primaryColor)
    }

    override fun draw() {
        if (!isVisible) {
            return
        }
        clear()
        push()
        rotate(-azimuth.value)
        drawCompass()
        drawLayers()
        drawReferencePoints()
        drawDestination()
        pop()
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            prefs.navigation.maxBeaconDistance /= detector.scaleFactor
            maxDistanceMeters = Distance.meters(prefs.navigation.maxBeaconDistance)
            maxDistanceBaseUnits = maxDistanceMeters.convertTo(prefs.baseDistanceUnits)
            layers.forEach { it.invalidate() }
            return true
        }
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            singleTapAction?.invoke()
            return super.onSingleTapConfirmed(e)
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

    // TODO: Don't calculate coordinate to pixel strategy on the fly
    override fun toPixel(coordinate: Coordinate): PixelCoordinate {
        return coordinateToPixelStrategy.getPixels(coordinate)
    }

    override fun toCoordinate(pixel: PixelCoordinate): Coordinate {
        TODO("Not yet implemented")
    }

    // TODO: Save meters per pixel
    override var metersPerPixel: Float
        get() = TODO("Not yet implemented")
        set(value) {}

    override val layerScale: Float = 1f

    override var mapCenter: Coordinate
        get() = _location
        set(value) {
            setLocation(value)
        }
    override var mapRotation: Float
        get() = azimuth.value
        set(value) {
            azimuth = Bearing(value)
        }
}