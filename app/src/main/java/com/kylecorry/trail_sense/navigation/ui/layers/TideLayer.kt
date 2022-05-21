package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.maps.ICoordinateToPixelStrategy
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.ui.CurrentTideData
import kotlin.reflect.KMutableProperty0

class TideLayer : ILayer {

    private val _tides = mutableListOf<Pair<TideTable, CurrentTideData>>()
    private var _highTideImg: Bitmap? = null
    private var _lowTideImg: Bitmap? = null
    private var _halfTideImg: Bitmap? = null
    private var _azimuth = Bearing(0f)

    fun setTides(tides: List<Pair<TideTable, CurrentTideData>>) {
        _tides.clear()
        _tides.addAll(tides)
        invalidate()
    }

    fun setAzimuth(azimuth: Bearing){
        _azimuth = azimuth
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, mapper: ICoordinateToPixelStrategy, scale: Float) {
        drawer.imageMode(ImageMode.Center)
        _tides.forEach { tide ->
            tide.first.location ?: return@forEach
            val center = mapper.getPixels(tide.first.location!!)
            val img = getImage(drawer, tide.second.type, scale)
            drawer.push()
            drawer.rotate(_azimuth.value, center.x, center.y)
            drawer.image(img, center.x, center.y)
            drawer.pop()
        }
        drawer.imageMode(ImageMode.Corner)
    }

    private fun getImage(drawer: ICanvasDrawer, type: TideType?, scale: Float): Bitmap {
        return when (type) {
            TideType.High -> _highTideImg ?: loadImage(
                R.drawable.ic_tide_high,
                drawer,
                scale,
                this::_highTideImg
            )
            TideType.Low -> _lowTideImg ?: loadImage(
                R.drawable.ic_tide_low,
                drawer,
                scale,
                this::_lowTideImg
            )
            null -> _halfTideImg ?: loadImage(
                R.drawable.ic_tide_half,
                drawer,
                scale,
                this::_halfTideImg
            )
        }
    }

    private fun loadImage(
        @DrawableRes id: Int,
        drawer: ICanvasDrawer,
        scale: Float,
        setter: KMutableProperty0<Bitmap?>
    ): Bitmap {
        val size = (drawer.dp(10f) * scale).toInt()
        val img = drawer.loadImage(id, size, size)
        setter.set(img)
        return img
    }

    override fun invalidate() {
        // Do nothing
    }

    protected fun finalize() {
        _halfTideImg?.recycle()
        _highTideImg?.recycle()
        _lowTideImg?.recycle()
        _halfTideImg = null
        _highTideImg = null
        _lowTideImg = null
    }
}