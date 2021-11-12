package com.kylecorry.trail_sense.navigation.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Circle
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.RadarCompassCoordinateToPixelStrategy
import com.kylecorry.trail_sense.navigation.domain.RenderedPath
import com.kylecorry.trail_sense.navigation.domain.RenderedPathFactory
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.maps.ICoordinateToPixelStrategy
import com.kylecorry.trail_sense.shared.paths.PathLineDrawerFactory
import kotlin.math.min

class RadarCompassView : BaseCompassView {
    private lateinit var center: PixelCoordinate
    private var compass: Bitmap? = null

    @ColorInt
    private var primaryColor: Int = Color.WHITE

    @ColorInt
    private var secondaryColor: Int = Color.WHITE

    private val formatService by lazy { FormatService(context) }

    private var iconSize = 0
    private var radarSize = 0
    private var directionSize = 0
    private var compassSize = 0
    private lateinit var compassPath: Path
    private var distanceSize = 0f
    private var cardinalSize = 0f

    private var metersPerPixel = 1f

    private lateinit var maxDistanceBaseUnits: Distance
    private lateinit var maxDistanceMeters: Distance
    private lateinit var coordinateToPixelStrategy: ICoordinateToPixelStrategy
    private var pathPool = ObjectPool { Path() }
    private var renderedPaths = mapOf<Long, RenderedPath>()
    private var pathsRendered = false

    private var singleTapAction: (() -> Unit)? = null

    private var north = ""
    private var south = ""
    private var east = ""
    private var west = ""

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

    override fun showPaths(paths: List<IMappablePath>) {
        _paths = paths
        pathsRendered = false
        invalidate()
    }

    private fun drawDestination() {
        val destination = _destination ?: return
        val color = destination.color
        push()
        fill(color)
        opacity(100)
        val dp2 = dp(2f)
        arc(
            iconSize.toFloat() + dp2,
            iconSize.toFloat() + dp2,
            compassSize.toFloat(),
            compassSize.toFloat(),
            _azimuth - 90,
            _azimuth - 90 + deltaAngle(_azimuth, destination.bearing.value),
            ArcMode.Pie
        )
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
    }

    private fun getDistanceFromCenter(pixel: PixelCoordinate): Float {
        return pixel.distanceTo(center)
    }

    override fun finalize() {
        super.finalize()
        tryOrNothing {
            compass?.recycle()
        }
    }

    private fun drawLocations() {
        _locations.forEach { drawLocation(it) }
    }

    private fun drawPaths() {
        if (!pathsRendered) {
            for (path in renderedPaths) {
                pathPool.release(path.value.path)
            }
            renderedPaths = generatePaths(_paths)
            pathsRendered = true
        }

        val factory = PathLineDrawerFactory()
        push()
        clip(compassPath)
        for (path in _paths) {
            val rendered = renderedPaths[path.id] ?: continue
            val drawer = factory.create(path.style)
            val centerPixel = coordinateToPixel(rendered.origin)
            push()
            translate(centerPixel.x, centerPixel.y)
            drawer.draw(this, path.color) {
                path(rendered.path)
            }
            pop()
        }
        pop()
        noStroke()
        fill(Color.WHITE)
        noPathEffect()
    }

    private fun drawLocation(location: IMappableLocation) {
        val pixel = coordinateToPixel(location.coordinate)
        if (getDistanceFromCenter(pixel) > compassSize / 2) {
            return
        }
        noTint()
        stroke(Color.WHITE)
        strokeWeight(dp(0.5f))
        fill(location.color)
        circle(pixel.x, pixel.y, radarSize.toFloat())
    }

    private fun drawReferencePoints() {
        _references.forEach { drawReferencePoint(it) }
    }

    private fun drawReferencePoint(reference: IMappableReferencePoint) {
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
        val bitmap = getBitmap(reference.drawableId, iconSize)
        image(bitmap, width / 2f - iconSize / 2f, 0f)
        pop()
        noTint()
        opacity(255)
        noStroke()
    }

    private fun drawCompass() {
        imageMode(ImageMode.Center)
        opacity(255)
        image(
            compass!!,
            width / 2f,
            height / 2f,
        )

        drawPaths()

        noFill()
        stroke(color(60))
        strokeWeight(3f)
        push()
        rotate(_azimuth)
        if (_destination == null) {
            line(width / 2f, height / 2f, width / 2f, iconSize + dp(2f))
        }
        image(getBitmap(R.drawable.ic_beacon, directionSize), width / 2f, height / 2f)
        stroke(color(100))
        circle(width / 2f, height / 2f, compassSize / 2f)
        circle(width / 2f, height / 2f, 3 * compassSize / 4f)
        circle(width / 2f, height / 2f, compassSize / 4f)

        fill(Color.WHITE)
        stroke(Resources.getAndroidColorAttr(context, R.attr.colorSecondary))

        val quarterDist = maxDistanceBaseUnits.times(0.25f).toRelativeDistance()
        val threeQuarterDist = maxDistanceBaseUnits.times(0.75f).toRelativeDistance()

        textSize(distanceSize)

        // TODO: This doesn't need to happen on every draw
        val quarterText = formatService.formatDistance(
            quarterDist,
            Units.getDecimalPlaces(quarterDist.units),
            false
        )
        val threeQuarterText = formatService.formatDistance(
            threeQuarterDist,
            Units.getDecimalPlaces(threeQuarterDist.units),
            false
        )

        textMode(TextMode.Center)
        text(quarterText, width / 2f + compassSize / 8f, height / 2f)
        text(threeQuarterText, width / 2f + 3 * compassSize / 8f, height / 2f)

        pop()

        textSize(cardinalSize)
        push()
        rotate(0f)
        fill(primaryColor)
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

    private fun coordinateToPixel(coordinate: Coordinate): PixelCoordinate {
        return coordinateToPixelStrategy.getPixels(coordinate)
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
        distanceSize = sp(8f)
        cardinalSize = sp(10f)
        primaryColor = Resources.getAndroidColorAttr(context, R.attr.colorPrimary)
        secondaryColor = Resources.getAndroidColorAttr(context, R.attr.colorSecondary)
        compass = loadImage(R.drawable.compass, compassSize, compassSize)
        maxDistanceMeters = Distance.meters(prefs.navigation.maxBeaconDistance)
        maxDistanceBaseUnits = maxDistanceMeters.convertTo(prefs.baseDistanceUnits)
        metersPerPixel = maxDistanceMeters.distance / (compassSize / 2f)
        north = context.getString(R.string.direction_north)
        south = context.getString(R.string.direction_south)
        east = context.getString(R.string.direction_east)
        west = context.getString(R.string.direction_west)
        center = PixelCoordinate(width / 2f, height / 2f)
        updateCoordinateToPixelStrategy()
    }

    override fun draw() {
        if (!isVisible) {
            return
        }
        updateCoordinateToPixelStrategy()
        clear()
        push()
        rotate(-_azimuth)
        drawCompass()
        drawLocations()
        drawReferencePoints()
        drawDestination()
        pop()
    }

    private fun generatePaths(paths: List<IMappablePath>): Map<Long, RenderedPath> {
        val factory = RenderedPathFactory(metersPerPixel, _location, _declination, _useTrueNorth)
        val map = mutableMapOf<Long, RenderedPath>()
        for (path in paths) {
            val pathObj = pathPool.get()
            pathObj.reset()
            map[path.id] = factory.render(path.points.map { it.coordinate }, pathObj)
        }
        return map
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            prefs.navigation.maxBeaconDistance *= detector.scaleFactor
            maxDistanceMeters = Distance.meters(prefs.navigation.maxBeaconDistance)
            maxDistanceBaseUnits = maxDistanceMeters.convertTo(prefs.baseDistanceUnits)
            metersPerPixel = maxDistanceMeters.distance / (compassSize / 2f)
            pathsRendered = false
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            super.onScaleEnd(detector)
            // TODO: Signal for the beacons to be rescanned
        }
    }

    private fun updateCoordinateToPixelStrategy() {
        coordinateToPixelStrategy = RadarCompassCoordinateToPixelStrategy(
            Circle(Vector2(center.x, center.y), compassSize / 2f),
            Geofence(_location, maxDistanceMeters),
            _useTrueNorth,
            _declination
        )
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
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
}