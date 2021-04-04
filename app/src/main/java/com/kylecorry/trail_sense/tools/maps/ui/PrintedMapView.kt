package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.CompassDirection
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.math.cosDegrees
import com.kylecorry.trailsensecore.domain.math.sinDegrees
import com.kylecorry.trailsensecore.domain.math.wrap
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


class PrintedMapView : View {
    private lateinit var paint: Paint
    private val icons = mutableMapOf<Int, Bitmap>()
    private var beacons = listOf<Beacon>()
    private var compass: Bitmap? = null
    private var isInit = false
    private var azimuth = Bearing(0f)
    private var map: Bitmap? = null
    private var myLocation = Coordinate.zero
    private var destination: Beacon? = null
    private var calibrationPoint1: MapCalibrationPoint? = null
    private var calibrationPoint2: MapCalibrationPoint? = null
    private var mapX = 0f
    private var mapY = 0f

    @ColorInt
    private var primaryColor: Int = Color.WHITE

    @ColorInt
    private var secondaryColor: Int = Color.WHITE

    private var iconSize = 0
    private var directionSize = 0
    private var compassSize = 0
    private var distanceSize = 0f

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

            mapX = min(width.toFloat(), max(mapX, -map!!.width.toFloat()))
            mapY = min(height.toFloat(), max(mapY, -map!!.height.toFloat()))
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            UiUtils.shortToast(context, "${x / scale - mapX}, ${y / scale - mapY}")
            println("${x / scale - mapX}, ${y / scale - mapY}")
        }
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale *= detector.scaleFactor
            scale = max(0.1f, min(scale, 5.0f))
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
            iconSize = dp(8f).toInt()
            directionSize = dp(14f).toInt()
            compassSize = min(height, width) - 2 * iconSize - 2 * dp(2f).toInt()
            isInit = true
            distanceSize = sp(8f)
            primaryColor = UiUtils.color(context, R.color.colorPrimary)
            secondaryColor = UiUtils.color(context, R.color.colorAccent)
            val compassDrawable = UiUtils.drawable(context, R.drawable.radar)
            compass = compassDrawable?.toBitmap(compassSize, compassSize)
            val mapDrawable = UiUtils.drawable(context, R.drawable.mount_washington)
            map = resize(mapDrawable?.toBitmap()!!, width, height)
            mapY = height / 2f - map!!.height / 2f
        }
        if (visibility != VISIBLE) {
            postInvalidateDelayed(20)
            invalidate()
            return
        }
        canvas.drawColor(Color.TRANSPARENT)
        canvas.scale(scale, scale)
        drawMap(canvas)
        drawDestination(canvas)
        drawCurrentPosition(canvas)
        drawBeacons(canvas)
        postInvalidateDelayed(20)
        invalidate()
    }

    private fun drawDestination(canvas: Canvas) {
        destination ?: return
        val myLocation = getPixelCoordinate(myLocation)
        val destLoc = getPixelCoordinate(destination!!.coordinate)
        if (myLocation != null && destLoc != null) {
            paint.color = primaryColor
            paint.strokeWidth = dp(2f)
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

    fun setScale(scale: Float){
        this.scale = scale
    }

    fun setMyLocation(location: Coordinate) {
        myLocation = location
    }

    fun setBeacons(beacons: List<Beacon>) {
        this.beacons = beacons
    }

    fun setDestination(beacon: Beacon?) {
        destination = beacon
    }

    fun setCalibrationPoints(first: MapCalibrationPoint, second: MapCalibrationPoint) {
        calibrationPoint1 = first
        calibrationPoint2 = second
    }

    fun recenter(){
        scale = 1f
        mapX = 0f
        mapY = height / 2f - map!!.height / 2f
    }

    private fun drawMap(canvas: Canvas) {
        map ?: return
        canvas.drawBitmap(
            map!!, mapX, mapY, paint
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
            paint.color = primaryColor
            paint.colorFilter = PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(
                getBitmap(R.drawable.ic_beacon, directionSize),
                mapX + myLocation.x - directionSize / 2f,
                mapY + myLocation.y - directionSize / 2f,
                paint
            )
            paint.colorFilter = null
            canvas.restore()
        }
    }

    private fun drawBeacons(canvas: Canvas) {
        for (beacon in beacons) {
            val coord = getPixelCoordinate(beacon.coordinate)
            if (coord != null) {
                if (beacon.id == destination?.id){
                    paint.color = secondaryColor
                } else {
                    paint.color = primaryColor
                }
                canvas.drawCircle(mapX + coord.x, mapY + coord.y, (iconSize / 2f) / scale, paint)
            }
        }
    }

    private fun getPixelCoordinate(coordinate: Coordinate): PixelCoordinate? {
        calibrationPoint1 ?: return null
        calibrationPoint2 ?: return null

        val latDiff = (calibrationPoint1!!.location.latitude - calibrationPoint2!!.location.latitude).absoluteValue
        val lngDiff = (calibrationPoint1!!.location.longitude - calibrationPoint2!!.location.longitude).absoluteValue
        val latDegPerPix = latDiff / (calibrationPoint1!!.pixel.y - calibrationPoint2!!.pixel.y).absoluteValue
        val lngDegPerPix = lngDiff / (calibrationPoint1!!.pixel.x - calibrationPoint2!!.pixel.x).absoluteValue

        // TODO: Handle southern hemisphere and the equator/meridian (maybe by calculating distance keeping lat/lng the same)
        val left = calibrationPoint1!!.location.longitude - calibrationPoint1!!.pixel.x * lngDegPerPix
        val top = calibrationPoint1!!.location.latitude + calibrationPoint1!!.pixel.y * latDegPerPix

        val y = ((top - coordinate.latitude) / latDegPerPix).toFloat()
        val x = ((coordinate.longitude - left) / lngDegPerPix).toFloat()

        if (x < 0 || x > (map?.width ?: 0)) {
            return null
        }

        if (y < 0 || y > (map?.height ?: 0)) {
            return null
        }

        return PixelCoordinate(x, y)
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

    private fun dp(size: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, size,
            resources.displayMetrics
        )
    }

    private fun sp(size: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, size,
            resources.displayMetrics
        )
    }


}