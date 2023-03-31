package com.kylecorry.trail_sense.navigation.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.ui.layers.compass.ICompassLayer
import com.kylecorry.trail_sense.navigation.ui.layers.compass.ICompassView
import com.kylecorry.trail_sense.shared.UserPreferences

abstract class BaseCompassView : CanvasView, INearbyCompassView, ICompassView {

    private val bitmapLoader by lazy { BitmapLoader(context) }
    protected var _location = Coordinate.zero
    protected var _useTrueNorth = false
    protected var _declination: Float = 0f
    protected val prefs by lazy { UserPreferences(context) }

    override val compassCenter: Coordinate
        get() = _location

    override val useTrueNorth: Boolean
        get() = _useTrueNorth

    override val declination: Float
        get() = _declination

    private val compassLayers = mutableListOf<ICompassLayer>()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        runEveryCycle = false
        setupAfterVisible = true
    }

    override var azimuth: Bearing = Bearing(0f)
        set(value) {
            field = value
            invalidate()
        }

    override fun setLocation(location: Coordinate) {
        _location = location
        invalidate()
    }

    override fun setDeclination(declination: Float) {
        _declination = declination
        invalidate()
    }

    protected open fun finalize() {
        bitmapLoader.clear()
    }

    protected fun getBitmap(@DrawableRes id: Int, size: Int): Bitmap {
        return bitmapLoader.load(id, size)
    }

    protected fun drawCompassLayers() {
        compassLayers.forEach { it.draw(this, this) }
    }

    protected fun invalidateCompassLayers() {
        compassLayers.forEach { it.invalidate() }
    }

    override fun setup() {
        _useTrueNorth = prefs.navigation.useTrueNorth
    }

    override fun addCompassLayer(layer: ICompassLayer) {
        compassLayers.add(layer)
    }

    override fun removeCompassLayer(layer: ICompassLayer) {
        compassLayers.remove(layer)
    }

    override fun setCompassLayers(layers: List<ICompassLayer>) {
        compassLayers.clear()
        compassLayers.addAll(layers)
    }
}