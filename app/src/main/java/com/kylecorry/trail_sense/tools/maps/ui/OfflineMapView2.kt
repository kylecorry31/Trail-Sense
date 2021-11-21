package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.ui.IMappableLocation
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.toPixel
import com.kylecorry.trail_sense.tools.maps.domain.Map
import kotlin.math.max
import kotlin.math.min


class OfflineMapView2 : SubsamplingScaleImageView {

    private lateinit var drawer: ICanvasDrawer
    private var isSetup = false
    private var myLocation: Coordinate? = null
    private var map: Map? = null
    private val geology = GeologyService()
    private var azimuth = 0f
    private var locations = emptyList<IMappableLocation>()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (!isReady || canvas == null) {
            return
        }

        if (!isSetup){
            drawer = CanvasDrawer(context, canvas)
            setup()
        }

        drawer.canvas = canvas
        draw()
    }

    fun setup(){

    }

    fun draw(){
        map ?: return
        drawMyLocation()
        drawLocations()
    }

    fun showMap(map: Map){
        val file = LocalFiles.getFile(context, map.filename, false)
        setImage(ImageSource.uri(file.toUri()))
        this.map = map
    }

    fun setMyLocation(coordinate: Coordinate?) {
        myLocation = coordinate
        invalidate()
    }

    private fun drawMyLocation() {
        val scale = min(1f, max(scale, 0.9f))
        val location = myLocation ?: return
        val pixels = getPixelCoordinate(location) ?: return

        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(drawer.dp(1f) / scale)
        drawer.fill(AppColor.Orange.color)
        drawer.push()
        drawer.rotate(azimuth, pixels.x, pixels.y)
        drawer.triangle(
            pixels.x, pixels.y - drawer.dp(6f) / scale,
            pixels.x - drawer.dp(5f) / scale, pixels.y + drawer.dp(6f) / scale,
            pixels.x + drawer.dp(5f) / scale, pixels.y + drawer.dp(6f) / scale
        )
        drawer.pop()
    }

    fun setAzimuth(azimuth: Float) {
        this.azimuth = azimuth
        invalidate()
    }

    fun showLocations(locations: List<IMappableLocation>) {
        this.locations = locations
        invalidate()
    }

    private fun drawLocations() {
        val scale = min(1f, max(scale, 0.9f))
        for (beacon in locations) {
            val coord = getPixelCoordinate(beacon.coordinate)
            if (coord != null) {
//                drawer.opacity(
//                    if (beacon.id == destination?.id || destination == null) {
//                        255
//                    } else {
//                        200
//                    }
//                )
                drawer.stroke(Color.WHITE)
                drawer.strokeWeight(drawer.dp(1f) / scale)
                drawer.fill(beacon.color)
                drawer.circle(coord.x, coord.y, drawer.dp(8f) / scale)
            }
        }
        drawer.opacity(255)
    }

    private fun getPixelCoordinate(
        coordinate: Coordinate,
        nullIfOffMap: Boolean = true
    ): PixelCoordinate? {

        val mapSize = sWidth.toFloat() to sHeight.toFloat()

        val bounds = map?.boundary(mapSize.first, mapSize.second) ?: return null

        val pixels = geology.toMercator(coordinate, bounds, mapSize).toPixel()

        if (nullIfOffMap && (pixels.x < 0 || pixels.x > mapSize.first)) {
            return null
        }

        if (nullIfOffMap && (pixels.y < 0 || pixels.y > mapSize.second)) {
            return null
        }

        val view = sourceToViewCoord(pixels.x, pixels.y)!!

        return PixelCoordinate(view.x, view.y)
    }

}