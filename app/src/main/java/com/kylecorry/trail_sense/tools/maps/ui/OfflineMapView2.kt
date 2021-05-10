package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.CanvasView
import com.kylecorry.trail_sense.tools.maps.domain.MapPixelBounds
import com.kylecorry.trail_sense.tools.maps.infrastructure.fixPerspective
import com.kylecorry.trail_sense.tools.maps.infrastructure.resize
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.infrastructure.images.BitmapUtils
import com.kylecorry.trailsensecore.infrastructure.persistence.LocalFileService
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.domain.geo.Path
import com.kylecorry.trailsensecore.domain.geo.cartography.Map
import com.kylecorry.trailsensecore.domain.pixels.*


class OfflineMapView2 : CanvasView {

    private var keepNorthUp = true
    private var azimuth = 0f
    private var map: Map? = null
    private var mapImage: Bitmap? = null
    private var translateX = 0f
    private var translateY = 0f
    private var scale = 1f

    // Features
    private var myLocation: Coordinate? = null
    private var beacons: List<Beacon> = listOf()
    private var destination: Beacon? = null

    @ColorInt
    private var primaryColor: Int = Color.BLACK

    private val fileService by lazy { LocalFileService(context) }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )


    override fun setup() {
        primaryColor = UiUtils.color(context, R.color.colorPrimary)
    }

    override fun draw() {
        val map = map ?: return
        if (mapImage == null) {
            mapImage = loadMap(map)
        }
        val mapImage = mapImage ?: return

        push()
        translate(translateX, translateY)
        scale(scale)
        if (!keepNorthUp) {
            rotate(azimuth) // TODO: Rotate around my position
        }
        image(mapImage, 0f, 0f)

        drawDestination()
        drawMyLocation()
        drawBeacons()

        pop()
    }

    fun showMap(map: Map) {
        this.map = map
        this.mapImage = null
    }

    fun setAzimuth(azimuth: Float, rotateMap: Boolean = false) {
        this.azimuth = azimuth
        this.keepNorthUp = !rotateMap
    }

    // TODO: Include error radius
    fun setMyLocation(coordinate: Coordinate?) {
        myLocation = coordinate
    }

    // TODO: Switch to layers
    fun showBeacons(beacons: List<Beacon>) {
        this.beacons = beacons
    }

    fun showDestination(destination: Beacon?) {
        this.destination = destination
    }

    fun showPaths(paths: List<Path>) {
        // TODO: Do this
    }


    private fun loadMap(map: Map): Bitmap {
        val file = fileService.getFile(map.filename, false)
        val bitmap = BitmapUtils.decodeBitmapScaled(
            file.path,
            width,
            height
        )
//        val resized = bitmap.resize(width, height)
//        bitmap.recycle()
//        return resized

        // TODO: Don't actually keep this here
        val mapBounds = MapPixelBounds(
            topLeft = PixelCoordinate(79.991455f, 316.9709f),
            topRight = PixelCoordinate(1056.9617f, 204.98181f),
            bottomLeft = PixelCoordinate(217.99072f, 1413.8954f),
            bottomRight = PixelCoordinate(1053.9624f, 1413.8954f)
        )
        val tempMapImage = bitmap.resize(width, height)
        val image = tempMapImage.fixPerspective(mapBounds)
        tempMapImage.recycle()
        return image
    }

    private fun drawMyLocation() {
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
            }
        }
        opacity(255)
    }

    private fun getPixelCoordinate(
        coordinate: Coordinate,
        nullIfOffMap: Boolean = true
    ): PixelCoordinate? {
        val mapImage = mapImage ?: return null

        val pixels =
            map?.getPixels(coordinate, mapImage.width.toFloat(), mapImage.height.toFloat())
                ?: return null

        if (nullIfOffMap && (pixels.x < 0 || pixels.x > mapImage.width)) {
            return null
        }

        if (nullIfOffMap && (pixels.y < 0 || pixels.y > mapImage.height)) {
            return null
        }

        return pixels
    }


    // Gesture detectors

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            translateX -= distanceX // scale
            translateY -= distanceY // scale
            return true
        }
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale *= detector.scaleFactor
//            scale = max(0.1f, min(scale, 8.0f))
            return true
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)
    private val mPanDetector = GestureDetector(context, mGestureListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)
        mPanDetector.onTouchEvent(event)
        return true
    }

}