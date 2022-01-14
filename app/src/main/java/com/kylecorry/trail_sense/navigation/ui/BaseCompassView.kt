package com.kylecorry.trail_sense.navigation.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences

abstract class BaseCompassView : CanvasView, INearbyCompassView {

    private val icons = mutableMapOf<Int, Bitmap>()
    protected var _azimuth = 0f
    protected var _destination: IMappableBearing? = null
    protected var _locations: List<IMappableLocation> = emptyList()
    protected var _highlightedLocation: IMappableLocation? = null
    protected var _paths: List<IMappablePath> = emptyList()
    protected var _references: List<IMappableReferencePoint> = emptyList()
    protected var _location = Coordinate.zero
    protected var _useTrueNorth = false
    protected var _declination: Float = 0f
    protected val prefs by lazy { UserPreferences(context) }

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

    override fun setAzimuth(azimuth: Bearing) {
        this._azimuth = azimuth.value
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

    override fun showLocations(locations: List<IMappableLocation>) {
        _locations = locations
        invalidate()
    }

    override fun showPaths(paths: List<IMappablePath>) {
        _paths = paths
        invalidate()
    }

    override fun showReferences(references: List<IMappableReferencePoint>) {
        _references = references
        invalidate()
    }

    override fun showDirection(bearing: IMappableBearing?) {
        _destination = bearing
        invalidate()
    }

    override fun highlightLocation(location: IMappableLocation?) {
        _highlightedLocation = location
        invalidate()
    }

    protected open fun finalize() {
        tryOrNothing {
            for (icon in icons) {
                icon.value.recycle()
            }
            icons.clear()
        }
    }

    protected fun getBitmap(@DrawableRes id: Int, size: Int): Bitmap {
        val bitmap = if (icons.containsKey(id)) {
            icons[id]
        } else {
            val drawable = Resources.drawable(context, id)
            val bm = drawable?.toBitmap(size, size)
            icons[id] = bm!!
            icons[id]
        }
        return bitmap!!
    }

    override fun setup() {
        _useTrueNorth = prefs.navigation.useTrueNorth
    }
}