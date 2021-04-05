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
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.domain.PixelCoordinate
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.infrastructure.persistence.LocalFileService
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlin.math.max
import kotlin.math.min


class PrintedMapView : View {
    private lateinit var paint: Paint
    private val icons = mutableMapOf<Int, Bitmap>()
    private var beacons = listOf<Beacon>()
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

    private val fileService by lazy { LocalFileService(context) }

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
                val coordinate = map?.getCoordinate(PixelCoordinate(xMap, yMap), mapImage!!.width.toFloat(), mapImage!!.height.toFloat())
                if (coordinate != null){
                    onSelectLocation?.invoke(coordinate)
                }
            }
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // TODO: Determine if a beacon was tapped, if so call a callback
            if (mapImage != null) {
                val xMap = e.x / scale - mapX
                val yMap = e.y / scale - mapY
                val percent = PercentCoordinate(xMap / mapImage!!.width, yMap / mapImage!!.height)
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
            iconSize = dp(8f).toInt()
            directionSize = dp(14f).toInt()
            compassSize = min(height, width) - 2 * iconSize - 2 * dp(2f).toInt()
            isInit = true
            distanceSize = sp(8f)
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
        if (mapImage == null && map != null){
            val file = fileService.getFile(map!!.filename, false)
            val bitmap = CustomUiUtils.decodeBitmapScaled(
                file.path,
                width,
                height
            )
//            val bitmap = BitmapFactory.decodeFile(file.path)
            // TODO: Scale instead of resize
            mapImage = resize(bitmap, width, height)
            recenter()
        }
        canvas.drawColor(Color.TRANSPARENT)
        canvas.scale(scale, scale)
        drawMap(canvas)
        drawDestination(canvas)
        drawCurrentPosition(canvas)
        drawBeacons(canvas)
        drawCalibrationPoints(canvas)
        postInvalidateDelayed(20)
        invalidate()
    }

    fun showCalibrationPoints(points: List<MapCalibrationPoint>? = null){
        calibrationPoints = points ?: map?.calibrationPoints ?: listOf()
        showCalibrationPoints = true
    }

    fun hideCalibrationPoints(){
        showCalibrationPoints = false
    }

    fun setMap(map: Map, refreshImage: Boolean = true){
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

    fun recenter(){
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
            // TODO: Resize based on scale
            canvas.drawBitmap(
                getBitmap(R.drawable.ic_my_location, directionSize),
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
                    // Do something special - like border or something
                }
                paint.color = Color.WHITE
                canvas.drawCircle(mapX + coord.x, mapY + coord.y, (iconSize / 2f + dp(1f)) / scale, paint)
                paint.color = primaryColor
                canvas.drawCircle(mapX + coord.x, mapY + coord.y, (iconSize / 2f) / scale, paint)
            }
        }
    }

    private fun drawCalibrationPoints(canvas: Canvas) {
        if (!showCalibrationPoints || mapImage == null) return
        for (point in calibrationPoints) {
            val coord = point.imageLocation.toPixels(mapImage!!.width.toFloat(), mapImage!!.height.toFloat())
            paint.color = Color.WHITE
            canvas.drawCircle(mapX + coord.x, mapY + coord.y, (iconSize / 2f + dp(1f)) / scale, paint)
            paint.color = secondaryColor
            canvas.drawCircle(mapX + coord.x, mapY + coord.y, (iconSize / 2f) / scale, paint)
        }
    }

    private fun getPixelCoordinate(coordinate: Coordinate): PixelCoordinate? {
        mapImage ?: return null
        return map?.getPixels(coordinate, mapImage!!.width.toFloat(), mapImage!!.height.toFloat())
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