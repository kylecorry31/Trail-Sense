package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.sol.math.SolMath.clamp
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.PathLineDrawerFactory
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.RenderedPath
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.RenderedPathFactory
import com.kylecorry.trail_sense.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.toPixel
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.infrastructure.getFitSize
import kotlin.math.max


class OfflineMapView : CanvasView {

    private var keepNorthUp = true
    private var azimuth = 0f
    private var map: Map? = null
    private var mapImage: Bitmap? = null
    private val mapPath = Path()
    private var mapSize = Pair(0f, 0f)
    private var translateX = 0f
    private var translateY = 0f
    private var scale = 1f

    private var beaconCircles: List<Pair<Beacon, PixelCircle>> = listOf()

    // Features
    private var myLocation: Coordinate? = null
    private var beacons: List<Beacon> = listOf()
    private var destination: Beacon? = null
    private var paths: List<IMappablePath>? = null
    private var calibrationPoints = listOf<MapCalibrationPoint>()
    private var showCalibrationPoints = false

    private var pathPool = ObjectPool { Path() }
    private var renderedPaths = mapOf<Long, RenderedPath>()
    private var pathsRendered = false

    // Listeners
    var onSelectLocation: ((coordinate: Coordinate) -> Unit)? = null
    var onSelectBeacon: ((beacon: Beacon) -> Unit)? = null
    var onMapImageClick: ((percent: PercentCoordinate) -> Unit)? = null

    private val geology = GeologyService()

    @ColorInt
    private var primaryColor: Int = Color.BLACK

    @ColorInt
    private var secondaryColor: Int = Color.BLACK

    private val prefs by lazy { UserPreferences(context) }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        runEveryCycle = false
    }


    override fun setup() {
        primaryColor = Resources.color(context, R.color.colorPrimary)
        secondaryColor = Resources.color(context, R.color.colorSecondary)
    }

    override fun draw() {
        val map = map ?: return
        if (mapImage == null) {
            mapImage = loadMap(map)
            val size = mapImage?.getFitSize(width, height)
            size?.let {
                translateX = (width - it.first) / 2f
                translateY = (height - it.second) / 2f
                mapPath.apply {
                    reset()
                    addRect(0f, 0f, size.first, size.second, Path.Direction.CW)
                }
            }
        }
        val mapImage = mapImage ?: return

        push()
        translate(translateX, translateY)
        scale(scale, scale, width / 2f, height / 2f)
        clip(mapPath)
        if (!keepNorthUp) {
            myLocation?.let {
                getPixelCoordinate(it, false)?.let { pos ->
                    rotate(-azimuth, pos.x, pos.y)
                }
            }

        }
        mapSize = mapImage.getFitSize(width, height)
        image(mapImage, 0f, 0f, mapSize.first, mapSize.second, 0f, 0f)

        drawPaths()
        drawDestination()
        drawMyLocation()
        drawBeacons()
        drawCalibrationPoints()

        pop()
    }

    private fun getVisiblePartOfMap(): Rect {
        var topLeft = toMapCoordinate(PixelCoordinate(0f, 0f))
        var bottomRight = toMapCoordinate(PixelCoordinate(width.toFloat(), height.toFloat()))

        topLeft = PixelCoordinate(
            clamp(topLeft.x, 0f, mapSize.first),
            clamp(topLeft.y, 0f, mapSize.second)
        )
        bottomRight = PixelCoordinate(
            clamp(bottomRight.x, 0f, mapSize.first),
            clamp(bottomRight.y, 0f, mapSize.second)
        )

        return Rect(
            topLeft.x.toInt(),
            topLeft.y.toInt(),
            bottomRight.x.toInt(),
            bottomRight.y.toInt()
        )
    }

    fun recenter() {
        val size = mapImage?.getFitSize(width, height)
        size?.let {
            translateX = (width - it.first) / 2f
            translateY = (height - it.second) / 2f
        }
        scale = 1f
    }


    private fun drawPaths() {
        val paths = paths ?: return
        if (!pathsRendered) {
            val metersPerPixel =
                map?.distancePerPixel(mapSize.first, mapSize.second)?.meters()?.distance ?: return
            for (path in renderedPaths) {
                pathPool.release(path.value.path)
            }
            renderedPaths = generatePaths(paths, metersPerPixel)
            pathsRendered = true
        }

        val factory = PathLineDrawerFactory()
        for (path in paths) {
            val rendered = renderedPaths[path.id] ?: continue
            val drawer = factory.create(path.style)
            val centerPixel = getPixelCoordinate(rendered.origin, false) ?: continue
            push()
            translate(centerPixel.x, centerPixel.y)
            drawer.draw(this, path.color, strokeScale = scale) {
                path(rendered.path)
            }
            pop()
        }
        noStroke()
        fill(Color.WHITE)
        noPathEffect()
    }

    private fun generatePaths(
        paths: List<IMappablePath>,
        metersPerPixel: Float
    ): kotlin.collections.Map<Long, RenderedPath> {
        val factory = RenderedPathFactory(metersPerPixel, myLocation, 0f, true)
        val map = mutableMapOf<Long, RenderedPath>()
        for (path in paths) {
            val pathObj = pathPool.get()
            pathObj.reset()
            map[path.id] = factory.render(path.points.map { it.coordinate }, pathObj)
        }
        return map
    }

    private fun drawCalibrationPoints() {
        if (!showCalibrationPoints) return
        for (point in calibrationPoints) {
            val coord = point.imageLocation.toPixels(
                mapSize.first,
                mapSize.second
            )
            stroke(Color.WHITE)
            strokeWeight(dp(1f) / scale)
            fill(secondaryColor)
            circle(coord.x, coord.y, dp(8f) / scale)
        }
    }


    fun showMap(map: Map, refresh: Boolean = true) {
        this.map = map
        if (refresh) {
            this.mapImage = null
        }
        invalidate()
    }

    fun setAzimuth(azimuth: Float, rotateMap: Boolean = false) {
        this.azimuth = azimuth
        this.keepNorthUp = !rotateMap
        invalidate()
    }

    // TODO: Include error radius
    fun setMyLocation(coordinate: Coordinate?) {
        myLocation = coordinate
        invalidate()
    }

    // TODO: Switch to layers
    fun showBeacons(beacons: List<Beacon>) {
        this.beacons = beacons
        invalidate()
    }

    fun showDestination(destination: Beacon?) {
        this.destination = destination
        invalidate()
    }

    fun showPaths(paths: List<IMappablePath>) {
        this.paths = paths
        pathsRendered = false
        invalidate()
    }

    private fun loadMap(map: Map): Bitmap {
        val file = LocalFiles.getFile(context, map.filename, false)
        if (prefs.navigation.useLowResolutionMaps) {
            return BitmapUtils.decodeBitmapScaled(file.path, width, height)
        }
        return BitmapUtils.decodeBitmapScaled(file.path, width * 4, height * 4)
    }

    private fun drawMyLocation() {
        val scale = max(scale, 1f)
        val location = myLocation ?: return
        val pixels = getPixelCoordinate(location) ?: return

        stroke(Color.WHITE)
        strokeWeight(dp(1f) / scale)
        fill(primaryColor)
        push()
        if (keepNorthUp) {
            rotate(azimuth, pixels.x, pixels.y)
        }
        triangle(
            pixels.x, pixels.y - dp(6f) / scale,
            pixels.x - dp(5f) / scale, pixels.y + dp(6f) / scale,
            pixels.x + dp(5f) / scale, pixels.y + dp(6f) / scale
        )
        pop()
    }

    private fun drawDestination() {
        val startLocation = myLocation ?: return
        val endLocation = destination?.coordinate ?: return
        val lineColor = destination?.color ?: return

        val startPixel = getPixelCoordinate(startLocation) ?: return
        val endPixel = getPixelCoordinate(endLocation) ?: return

        noFill()
        stroke(lineColor)
        strokeWeight(dp(4f) / scale)
        opacity(127)
        line(startPixel.x, startPixel.y, endPixel.x, endPixel.y)
        opacity(255)
    }

    private fun drawBeacons() {
        val circles = mutableListOf<Pair<Beacon, PixelCircle>>()
        for (beacon in beacons) {
            val coord = getPixelCoordinate(beacon.coordinate)
            if (coord != null) {
                opacity(
                    if (beacon.id == destination?.id || destination == null) {
                        255
                    } else {
                        200
                    }
                )
                stroke(Color.WHITE)
                strokeWeight(dp(1f) / scale)
                fill(beacon.color)
                circle(coord.x, coord.y, dp(8f) / scale)
                circles.add(
                    beacon to PixelCircle(
                        PixelCoordinate(coord.x, coord.y),
                        3 * dp(4f) / scale
                    )
                )
            }
        }
        beaconCircles = circles
        opacity(255)
    }

    fun showCalibrationPoints(points: List<MapCalibrationPoint>? = null) {
        calibrationPoints = points ?: map?.calibrationPoints ?: listOf()
        showCalibrationPoints = true
        invalidate()
    }

    fun hideCalibrationPoints() {
        showCalibrationPoints = false
        invalidate()
    }

    private fun getPixelCoordinate(
        coordinate: Coordinate,
        nullIfOffMap: Boolean = true
    ): PixelCoordinate? {

        val bounds = map?.boundary(mapSize.first, mapSize.second) ?: return null

        val pixels = geology.toMercator(coordinate, bounds, mapSize).toPixel()

        if (nullIfOffMap && (pixels.x < 0 || pixels.x > mapSize.first)) {
            return null
        }

        if (nullIfOffMap && (pixels.y < 0 || pixels.y > mapSize.second)) {
            return null
        }

        return pixels
    }

    private fun keepMapOnScreen() {

        var wasOffScreen: Boolean
        var iterations = 0
        val maxIterations = 100

        do {
            wasOffScreen = false
            val bounds = getVisiblePartOfMap()
            // TODO: Calculate how much to move on screen
            if (bounds.width() == 0) {
                if (translateX < 0) {
                    translateX++
                } else {
                    translateX--
                }
                wasOffScreen = true
            }

            if (bounds.height() == 0) {
                if (translateY < 0) {
                    translateY++
                } else {
                    translateY--
                }
                wasOffScreen = true
            }
            iterations++
        } while (wasOffScreen && iterations < maxIterations)

    }


    // Gesture detectors

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            translateX -= distanceX
            translateY -= distanceY

            keepMapOnScreen()

            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            val x = e.x
            val y = e.y

            if (mapImage != null) {
                val mapCoords = toMapCoordinate(PixelCoordinate(x, y))
                val coordinate = map?.getCoordinate(
                    mapCoords,
                    mapSize.first,
                    mapSize.second
                )
                if (coordinate != null) {
                    onSelectLocation?.invoke(coordinate)
                }
            }
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            // TODO: Zoom to the place tapped
            zoom(2F)
            return super.onDoubleTap(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (mapImage != null) {
                val mapCoords = toMapCoordinate(PixelCoordinate(e.x, e.y))
                val circles = beaconCircles.sortedBy { it.second.center.distanceTo(mapCoords) }
                for (circle in circles) {
                    if (circle.second.contains(mapCoords)) {
                        onSelectBeacon?.invoke(circle.first)
                        return super.onSingleTapConfirmed(e)
                    }
                }

                val percent = PercentCoordinate(
                    mapCoords.x / mapSize.first,
                    mapCoords.y / mapSize.second
                )
                onMapImageClick?.invoke(percent)
            }
            return super.onSingleTapConfirmed(e)
        }
    }

    private fun toMapCoordinate(screen: PixelCoordinate): PixelCoordinate {
        val matrix = Matrix()
        val point = floatArrayOf(screen.x, screen.y)
        matrix.postScale(scale, scale, width / 2f, height / 2f)
        matrix.postTranslate(translateX, translateY)
        matrix.invert(matrix)
        matrix.mapPoints(point)
        return PixelCoordinate(point[0], point[1])
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoom(detector.scaleFactor)
            return true
        }
    }

    fun zoom(factor: Float) {
        scale *= factor
        translateX *= factor
        translateY *= factor
        keepMapOnScreen()
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)
    private val mPanDetector = GestureDetector(context, mGestureListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)
        mPanDetector.onTouchEvent(event)
        invalidate()
        return true
    }

}