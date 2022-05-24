package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Bitmap
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R

class MyLocationLayer : ILayer {

    private var _location: Coordinate? = null
    private var _azimuth: Bearing? = null
    private var _image: Bitmap? = null

    fun setLocation(location: Coordinate) {
        _location = location
        invalidate()
    }

    fun setAzimuth(azimuth: Bearing){
        _azimuth = azimuth
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        val scale = 1f // TODO: Determine this based on map.scale
        val point = map.toPixel(_location ?: map.center)
        // TODO: Handle tint
        drawer.opacity(255)
        drawer.imageMode(ImageMode.Center)
        drawer.push()
        drawer.rotate(_azimuth?.value ?: map.rotation.value, point.x, point.y)
        val image =
            _image ?: drawer.loadImage(R.drawable.ic_beacon, (drawer.dp(16f) * scale).toInt(), (drawer.dp(16f) * scale).toInt())
        _image = image
        drawer.image(image, point.x, point.y)
        drawer.pop()
        drawer.imageMode(ImageMode.Corner)
    }

    override fun invalidate() {
        // Do nothing
    }

    protected fun finalize() {
        _image?.recycle()
        _image = null
    }
}