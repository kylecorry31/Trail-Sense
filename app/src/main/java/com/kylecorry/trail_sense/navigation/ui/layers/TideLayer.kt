package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.ui.markers.BitmapMarker
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.ui.CurrentTideData
import kotlin.reflect.KMutableProperty0

class TideLayer : BaseLayer() {

    private val _tides = mutableListOf<Pair<TideTable, CurrentTideData>>()
    private var _highTideImg: Bitmap? = null
    private var _lowTideImg: Bitmap? = null
    private var _halfTideImg: Bitmap? = null

    fun setTides(tides: List<Pair<TideTable, CurrentTideData>>) {
        _tides.clear()
        _tides.addAll(tides)
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        clearMarkers()
        _tides.forEach { tide ->
            tide.first.location ?: return@forEach
            val img = getImage(drawer, tide.second.type)
            addMarker(BitmapMarker(tide.first.location!!, img))
        }
        super.draw(drawer, map)
    }

    private fun getImage(drawer: ICanvasDrawer, type: TideType?): Bitmap {
        return when (type) {
            TideType.High -> _highTideImg ?: loadImage(
                R.drawable.ic_tide_high,
                drawer,
                this::_highTideImg
            )
            TideType.Low -> _lowTideImg ?: loadImage(
                R.drawable.ic_tide_low,
                drawer,
                this::_lowTideImg
            )
            null -> _halfTideImg ?: loadImage(
                R.drawable.ic_tide_half,
                drawer,
                this::_halfTideImg
            )
        }
    }

    private fun loadImage(
        @DrawableRes id: Int,
        drawer: ICanvasDrawer,
        setter: KMutableProperty0<Bitmap?>
    ): Bitmap {
        val size = drawer.dp(10f).toInt()
        val img = drawer.loadImage(id, size, size)
        setter.set(img)
        return img
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