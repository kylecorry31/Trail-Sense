package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Bitmap
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.ui.markers.BitmapMarker

class MyLocationLayer : BaseLayer() {

    private var _location: Coordinate? = null
    private var _azimuth: Bearing? = null
    private var _image: Bitmap? = null

    fun setLocation(location: Coordinate) {
        _location = location
        invalidate()
    }

    fun setAzimuth(azimuth: Bearing) {
        _azimuth = azimuth
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        val size = drawer.dp(16f).toInt()
        val image = _image ?: drawer.loadImage(R.drawable.ic_beacon, size, size)
        _image = image
        clearMarkers()
        addMarker(BitmapMarker(_location ?: map.mapCenter, image, 16f, (_azimuth?.value ?: 0f) + map.mapRotation))
        super.draw(drawer, map)
    }

    protected fun finalize() {
        _image?.recycle()
        _image = null
    }
}