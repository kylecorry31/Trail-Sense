package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.Path
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.cartography.MapCalibrationPoint
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.pixels.PercentCoordinate
import com.kylecorry.trailsensecore.domain.pixels.PixelCircle
import com.kylecorry.trailsensecore.domain.pixels.PixelCoordinate
import com.kylecorry.trailsensecore.infrastructure.canvas.DottedPathEffect
import com.kylecorry.trailsensecore.infrastructure.canvas.getMaskedBitmap
import com.kylecorry.trailsensecore.infrastructure.images.BitmapUtils
import com.kylecorry.trailsensecore.infrastructure.persistence.LocalFileService
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class OfflineMapView : View {
    private lateinit var paint: Paint
    private val icons = mutableMapOf<Int, Bitmap>()
    private var beacons = listOf<Beacon>()
    private var paths = listOf<Path>()
    private var pathLines: List<PixelLine>? = null
    private var calibrationPoints = listOf<MapCalibrationPoint>()
    private var showCalibrationPoints = false
    private var compass: Bitmap? = null
    private var isInit = false
    private var azimuth = Bearing(0f)
    private var mapImage: Bitmap? = null
    private var map: Map? = null
    private var myLocation = Coordinate.zero
    private var destination: Beacon? = null
    private var mapX = 0f
    private var mapY = 0f

    private var beaconCircles = listOf<Pair<Beacon, PixelCircle>>()

    private val fileService by lazy { LocalFileService(context) }
    private val prefs by lazy { UserPreferences(context) }

    var onSelectLocation: ((coordinate: Coordinate) -> Unit)? = null
    var onSelectBeacon: ((beacon: Beacon) -> Unit)? = null
    var onMapImageClick: ((percent: PercentCoordinate) -> Unit)? = null

    @ColorInt
    private var primaryColor: Int = Color.WHITE

    @ColorInt
    private var secondaryColor: Int = Color.WHITE

    private var iconSize = 0
    private var directionSize = 0
    private var compassSize = 0

    private var scale = 1f

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            mapX -= distanceX / scale
            mapY -= distanceY / scale

            mapX = min(width.toFloat(), max(mapX, -mapImage!!.width.toFloat()))
            mapY = min(height.toFloat(), max(mapY, -mapImage!!.height.toFloat()))
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            val x = e.x
            val y = e.y

            if (mapImage != null) {
                val xMap = x / scale - mapX
                val yMap = y / scale - mapY
                val coordinate = map?.getCoordinate(
                    PixelCoordinate(xMap, yMap),
                    mapImage!!.width.toFloat(),
                    mapImage!!.height.toFloat()
                )
                if (coordinate != null) {
                    onSelectLocation?.invoke(coordinate)
                }
            }
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (mapImage != null) {
                val mapCoords = toMapCoordinate(PixelCoordinate(e.x, e.y))
                val relativeClick = PixelCoordinate(e.x / scale, e.y / scale)
                val circles = beaconCircles.sortedBy { it.second.center.distanceTo(relativeClick) }
                for (circle in circles) {
                    if (circle.second.contains(relativeClick)) {
                        onSelectBeacon?.invoke(circle.first)
                        return super.onSingleTapConfirmed(e)
                    }
                }

                val percent = PercentCoordinate(mapCoords.x / mapImage!!.width, mapCoords.y / mapImage!!.height)
                onMapImageClick?.invoke(percent)
            }
            return super.onSingleTapConfirmed(e)
        }
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale *= detector.scaleFactor
            scale = max(0.1f, min(scale, 8.0f))
            return true
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)
    private val mPanDetector = GestureDetector(context, mGestureListener)

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onDraw(canvas: Canvas) {
        if (!isInit) {
            paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.textAlign = Paint.Align.CENTER
            iconSize = UiUtils.dp(context, 8f).toInt()
            directionSize = UiUtils.dp(context, 16f).toInt()
            compassSize = min(height, width) - 2 * iconSize - 2 * UiUtils.dp(context, 2f).toInt()
            isInit = true
            primaryColor = UiUtils.color(context, R.color.colorPrimary)
            secondaryColor = UiUtils.color(context, R.color.colorAccent)
            val compassDrawable = UiUtils.drawable(context, R.drawable.radar)
            compass = compassDrawable?.toBitmap(compassSize, compassSize)
        }
        if (visibility != VISIBLE) {
            postInvalidateDelayed(20)
            invalidate()
            return
        }
        if (mapImage == null && map != null) {
            // TODO: Determine tiles for when zoom is > X (they should be X by X pixels, smaller around the edges
            // TODO: Load all visible tiles and overlay them over the map, unload them if zoomed out or out of view
            val file = fileService.getFile(map!!.filename, false)
            val bitmap = BitmapUtils.decodeBitmapScaled(
                file.path,
                width,
                height
            )
//            val bitmap = BitmapFactory.decodeFile(file.path)
            // TODO: Scale instead of resize
            mapImage = resize(bitmap, width, height)
            recenter()
        }

        if (pathLines == null && paths.isNotEmpty() && mapImage != null) {
            createPathLines()
        }

        canvas.drawColor(Color.TRANSPARENT)
        canvas.scale(scale, scale)
        drawMap(canvas)
        drawPaths(canvas)
        drawDestination(canvas)
        drawCurrentPosition(canvas)
        drawBeacons(canvas)
        drawCalibrationPoints(canvas)
        postInvalidateDelayed(20)
        invalidate()
    }

    fun showCalibrationPoints(points: List<MapCalibrationPoint>? = null) {
        calibrationPoints = points ?: map?.calibrationPoints ?: listOf()
        showCalibrationPoints = true
    }

    fun hideCalibrationPoints() {
        showCalibrationPoints = false
    }

    fun setMap(map: Map, refreshImage: Boolean = true) {
        this.map = map
        if (refreshImage) {
            mapImage = null
        }
    }

    private fun drawDestination(canvas: Canvas) {
        destination ?: return
        val myLocation = getPixelCoordinate(myLocation)
        val destLoc = getPixelCoordinate(destination!!.coordinate)
        if (myLocation != null && destLoc != null) {
            paint.color = primaryColor
            paint.strokeWidth = 6f / scale
            paint.alpha = 127
            paint.style = Paint.Style.STROKE
            canvas.drawLine(
                mapX + myLocation.x,
                mapY + myLocation.y,
                mapX + destLoc.x,
                mapY + destLoc.y,
                paint
            )
            paint.style = Paint.Style.FILL
            paint.alpha = 255
        }
    }

    fun setAzimuth(bearing: Bearing) {
        azimuth = bearing
    }

    fun setScale(scale: Float) {
        this.scale = scale
    }

    fun setMyLocation(location: Coordinate) {
        myLocation = location
    }

    fun setBeacons(beacons: List<Beacon>) {
        this.beacons = beacons
    }

    fun setPaths(paths: List<Path>) {
        this.paths = paths
        this.pathLines = null
    }

    fun setDestination(beacon: Beacon?) {
        destination = beacon
    }

    fun finalize() {
        mapImage?.recycle()
    }

    fun recenter() {
        scale = 1f
        mapX = 0f
        mapY = if (mapImage != null) {
            height / 2f - mapImage!!.height / 2f
        } else {
            height / 2f
        }
    }

    private fun drawMap(canvas: Canvas) {
        mapImage ?: return
        canvas.drawBitmap(
            mapImage!!, mapX, mapY, paint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)
        mPanDetector.onTouchEvent(event)
        return true
    }

    private fun drawCurrentPosition(canvas: Canvas) {
        val myLocation = getPixelCoordinate(myLocation)
        if (myLocation != null) {
            canvas.save()
            canvas.rotate(azimuth.value, mapX + myLocation.x, mapY + myLocation.y)
            canvas.scale(1 / scale, 1 / scale)
            // TODO: Resize based on scale
            canvas.drawBitmap(
                getBitmap(R.drawable.ic_my_location, directionSize),
                (mapX + myLocation.x) * scale - directionSize / 2f,
                (mapY + myLocation.y) * scale - directionSize / 2f,
                paint
            )
            paint.colorFilter = null
            canvas.restore()
        }
    }

    private fun drawBeacons(canvas: Canvas) {
        val circles = mutableListOf<Pair<Beacon, PixelCircle>>()
        for (beacon in beacons) {
            val coord = getPixelCoordinate(beacon.coordinate)
            if (coord != null) {
                val alpha = if (beacon.id == destination?.id || destination == null) {
                    255
                } else {
                    200
                }
                paint.color = Color.WHITE
                paint.alpha = alpha
                circles.add(
                    beacon to PixelCircle(
                        PixelCoordinate(mapX + coord.x, mapY + coord.y),
                        3 * (iconSize / 2f + UiUtils.dp(context, 1f)) / scale
                    )
                )
                canvas.drawCircle(
                    mapX + coord.x,
                    mapY + coord.y,
                    (iconSize / 2f + UiUtils.dp(context, 1f)) / scale,
                    paint
                )
                paint.color = primaryColor
                paint.alpha = alpha
                canvas.drawCircle(mapX + coord.x, mapY + coord.y, (iconSize / 2f) / scale, paint)
                paint.alpha = 255
            }
        }
        beaconCircles = circles
    }

    private fun drawPaths(canvas: Canvas) {
        mapImage ?: return

        if (paths.isEmpty()) {
            return
        }

        // TODO: Draw this on a masked bitmap
        val dotted = DottedPathEffect(3f / scale, 10f / scale)
        for (line in pathLines ?: listOf()) {
            if (line.dotted) {
                paint.pathEffect = dotted
                paint.style = Paint.Style.FILL
            } else {
                paint.pathEffect = null
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f / scale
            }
            paint.color = line.color
            paint.alpha = line.alpha

            canvas.drawLine(
                mapX + line.start.x,
                mapY + line.start.y,
                mapX + line.end.x,
                mapY + line.end.y,
                paint
            )
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
        paint.pathEffect = null
    }

    private fun drawCalibrationPoints(canvas: Canvas) {
        if (!showCalibrationPoints || mapImage == null) return
        for (point in calibrationPoints) {
            val coord = point.imageLocation.toPixels(
                mapImage!!.width.toFloat(),
                mapImage!!.height.toFloat()
            )
            paint.color = Color.WHITE
            canvas.drawCircle(
                mapX + coord.x,
                mapY + coord.y,
                (iconSize / 2f + UiUtils.dp(context, 1f)) / scale,
                paint
            )
            paint.color = secondaryColor
            canvas.drawCircle(mapX + coord.x, mapY + coord.y, (iconSize / 2f) / scale, paint)
        }
    }

    private fun getPixelCoordinate(
        coordinate: Coordinate,
        nullIfOffMap: Boolean = true
    ): PixelCoordinate? {
        mapImage ?: return null
        val pixels =
            map?.getPixels(coordinate, mapImage!!.width.toFloat(), mapImage!!.height.toFloat())
                ?: return null

        if (nullIfOffMap && (pixels.x < 0 || pixels.x > mapImage!!.width)) {
            return null
        }

        if (nullIfOffMap && (pixels.y < 0 || pixels.y > mapImage!!.height)) {
            return null
        }

        return pixels
    }

    private fun resize(image: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        return if (maxHeight > 0 && maxWidth > 0) {
            val width = image.width
            val height = image.height
            val ratioBitmap = width.toFloat() / height.toFloat()
            val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
            var finalWidth = maxWidth
            var finalHeight = maxHeight
            if (ratioMax > ratioBitmap) {
                finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
            } else {
                finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
            }
            Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
        } else {
            image
        }
    }

    private fun createPathLines() {
        val lines = mutableListOf<PixelLine>()
        val maxTimeAgo = prefs.navigation.showBacktrackPathDuration.seconds.toFloat()
        for (path in paths) {
            val pixelWaypoints = path.points.map {
                Pair(getPixelCoordinate(it.coordinate, false)!!, it.time)
            }
            val now = Instant.now().toEpochMilli()
            for (i in 1 until pixelWaypoints.size) {
                val hasTime = pixelWaypoints[i - 1].second != null
                val timeAgo =
                    if (hasTime) abs(now - pixelWaypoints[i - 1].second!!.toEpochMilli()) / 1000f else 0f
                val line = PixelLine(
                    pixelWaypoints[i - 1].first,
                    pixelWaypoints[i].first,
                    path.color,
                    if (!hasTime) 255 else (255 * (1 - timeAgo / maxTimeAgo)).toInt()
                        .coerceAtLeast(60),
                    path.dotted
                )
                lines.add(line)
            }
        }
        pathLines = lines
    }
    
    private fun toMapCoordinate(screen: PixelCoordinate): PixelCoordinate {
        return PixelCoordinate(screen.x / scale - mapX, screen.y / scale - mapY)
    }

    private fun getBitmap(@DrawableRes id: Int, size: Int = iconSize): Bitmap {
        val bitmap = if (icons.containsKey(id)) {
            icons[id]
        } else {
            val drawable = UiUtils.drawable(context, id)
            val bm = drawable?.toBitmap(size, size)
            icons[id] = bm!!
            icons[id]
        }
        return bitmap!!
    }

    internal data class PixelLine(
        val start: PixelCoordinate,
        val end: PixelCoordinate,
        @ColorInt val color: Int,
        val alpha: Int,
        val dotted: Boolean
    )


}