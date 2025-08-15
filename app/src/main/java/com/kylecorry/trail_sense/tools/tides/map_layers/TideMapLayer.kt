package com.kylecorry.trail_sense.tools.tides.map_layers

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.navigation.ui.markers.BitmapMapMarker
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import kotlin.reflect.KMutableProperty0

class TideMapLayer : BaseLayer() {

    private val _tides = mutableListOf<Pair<TideTable, TideType?>>()
    private var _highTideImg: Bitmap? = null
    private var _lowTideImg: Bitmap? = null
    private var _halfTideImg: Bitmap? = null

    private val lock = Any()

    private var opacity: Int = 255

    fun setPreferences(prefs: TideMapLayerPreferences) {
        opacity = SolMath.map(
            prefs.opacity.get().toFloat(),
            0f,
            100f,
            0f,
            255f,
            shouldClamp = true
        ).toInt()
        invalidate()
    }

    fun setTides(tides: List<Pair<TideTable, TideType?>>) {
        synchronized(lock) {
            _tides.clear()
            _tides.addAll(tides)
        }
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        drawer.opacity(opacity)
        clearMarkers()
        val tides = synchronized(lock) { _tides.toList() }
        tides.forEach { tide ->
            tide.first.location ?: return@forEach
            val img = getImage(drawer, tide.second)
            addMarker(BitmapMapMarker(tide.first.location!!, img))
        }
        super.draw(drawer, map)
        drawer.opacity(255)
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
        val size = drawer.dp(12f).toInt()
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