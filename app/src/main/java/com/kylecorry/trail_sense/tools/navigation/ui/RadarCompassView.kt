package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import com.kylecorry.andromeda.canvas.ArcMode
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.canvas.TextStyle
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Circle
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.tools.map.ui.MapView
import kotlin.math.min

class RadarCompassView : BaseCompassView {
    private var centerPixel: PixelCoordinate = PixelCoordinate(0f, 0f)
    private lateinit var compassCircle: Circle

    @ColorInt
    private var primaryColor: Int = Color.WHITE

    @ColorInt
    private var secondaryColor: Int = Color.WHITE

    @ColorInt
    private var textColor: Int = Color.WHITE

    private val formatService by lazy { FormatService.getInstance(context) }

    private var mapView: MapView? = null

    private var iconSize = 0
    private var radarSize = 0
    private var directionSize = 0
    private var compassSize = 0
    private lateinit var compassPath: Path
    private var distanceSize = 0f
    private var cardinalSize = 0f
    private var locationStrokeWeight = 0f
    private var dialTickRadius = 0f
    private val drawDialBezel = prefs.navigation.drawDialBezel
    private var maxDistanceBaseUnits: Distance = Distance.meters(0f)
    private var maxDistanceMeters: Distance = Distance.meters(0f)
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

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private fun drawAzimuth() {
        fill(Color.WHITE)
        tint(
            if (drawDialBezel) Color.WHITE.withAlpha(220) else Resources.androidTextColorPrimary(
                context
            )
        )
        imageMode(ImageMode.Corner)
        image(
            getBitmap(R.drawable.ic_arrow_target, iconSize),
            width / 2f - iconSize / 2f,
            0f
        )
        noTint()
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
        if (shouldDrawDial && shouldDrawAzimuthIndicator) {
            line(width / 2f, height / 2f, width / 2f, iconSize + dp(2f))
        }
        circle(width / 2f, height / 2f, compassSize.toFloat())
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

    fun setRadiusDistance(distance: Distance) {
        maxDistanceMeters = distance.meters()
        maxDistanceBaseUnits = maxDistanceMeters.convertTo(prefs.baseDistanceUnits)
        distanceText = null
        invalidate()
    }

    private fun drawCardinalDirections() {
        textMode(TextMode.Center)
        textSize(cardinalSize)
        stroke(secondaryColor)
        textStyle(TextStyle.Bold)
        strokeWeight(dp(1f))
        opacity(255)
        fill(primaryColor)
        drawDirection(0f, north)
        fill(Color.WHITE)
        drawDirection(90f, east)
        drawDirection(180f, south)
        drawDirection(270f, west)
    }

    private fun drawDirection(degrees: Float, text: String) {
        push()
        rotate(degrees)
        textMode(TextMode.Center)
        text(
            text,
            width / 2f,
            height / 2f - dialTickRadius + dp(1f)
        )
        pop()
    }

    fun bindMapView(view: MapView) {
        mapView = view
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
        distanceText = null
        north = context.getString(R.string.direction_north)
        south = context.getString(R.string.direction_south)
        east = context.getString(R.string.direction_east)
        west = context.getString(R.string.direction_west)
        centerPixel = PixelCoordinate(width / 2f, height / 2f)
        compassCircle = Circle(Vector2(centerPixel.x, centerPixel.y), compassSize / 2f)
        locationStrokeWeight = dp(0.5f)
        dialTickRadius = if (drawDialBezel) {
            compassSize / 2f + iconSize / 2f + dp(2f)
        } else {
            (compassSize / 2f) * 0.9f
        }
        dial = CompassDial(
            centerPixel,
            dialTickRadius,
            compassSize / 2f,
            Color.TRANSPARENT,
            Color.WHITE,
            Color.WHITE,
            hideTrueCardinalTicks = true,
            tickLength = dp(6f),
            cardinalTickLength = dp(10f),
            bezelThickness = if (drawDialBezel) (min(height, width) - compassSize) / 2f else 0f,
            bezelColor = secondaryColor

        )
        lastWidth = width
        lastHeight = height
        mapView?.clipPath = compassPath
    }

    override fun draw() {
        if (!isVisible) {
            return
        }
        if (lastWidth != width || lastHeight != height) {
            setup()
        }
        clear()
        push()
        rotate(-azimuth)
        dial.draw(drawer, false)
        if (shouldDrawAzimuthIndicator) {
            push()
            rotate(azimuth)
            drawAzimuth()
            pop()
        }
        drawCompassLayers()
        drawCompass()
        pop()
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
        imageMode(ImageMode.Center)
        translate(width / 2f, iconSize / 2f + dp(1f))
        rotate(reference.rotation, 0f, 0f)
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
            val pixel = mapView?.toPixel(it) ?: return@let
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
}
