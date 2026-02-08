package com.kylecorry.trail_sense.tools.photo_maps.ui

import android.content.Context
import android.util.AttributeSet
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.views.EnhancedImageView
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import kotlin.math.max
import kotlin.math.min


abstract class BasePhotoMapView : EnhancedImageView {

    protected var map: PhotoMap? = null
    private var fullResolutionPixels = 1f
    private val files = FileSubsystem.getInstance(context)

    private var shouldRecenter = true

    var onImageLoadedListener: (() -> Unit)? = null

    private fun getScale(resolutionPixels: Float): Float {
        return fullResolutionPixels / resolutionPixels
    }

    var mapAzimuth: Float = 0f
        set(value) {
            val newValue = if (keepMapUp) {
                -mapRotation
            } else {
                value
            }
            val changed = field != newValue
            field = newValue
            if (changed) {
                imageRotation = newValue
                invalidate()
            }
        }

    /**
     * Determines if the map should be kept with its top parallel to the top of the screen
     */
    var keepMapUp: Boolean = false
        set(value) {
            field = value
            if (value) {
                mapAzimuth = 0f
            }
            invalidate()
        }

    var mapRotation: Float = 0f
        protected set(value) {
            field = value
            invalidate()
        }

    var azimuth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    val layerScale: Float
        get() = min(1f, max(scale, 0.9f))

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun setup() {
        super.setup()
        setBackgroundColor(Resources.color(context, R.color.colorSecondary))
    }

    override fun draw() {
        super.draw()
        val map = map ?: return

        if (!map.isCalibrated) {
            return
        }
        maxScale = getScale(0.1f).coerceAtLeast(2 * minScale)
        if (shouldRecenter && isImageLoaded) {
            recenter()
            shouldRecenter = false
            onImageLoadedListener?.invoke()
        }
    }

    open fun showMap(map: PhotoMap) {
        this.map = map
        val rotation = map.calibration.rotation
        mapRotation = SolMath.deltaAngle(rotation, map.baseRotation().toFloat())
        fullResolutionPixels = map.distancePerPixel()?.meters()?.value ?: 1f
        if (keepMapUp) {
            mapAzimuth = 0f
        }

        if (files.get(map.pdfFileName).exists()) {
            setImage(map.pdfFileName, rotation)
        } else {
            setImage(map.filename, rotation)
        }
    }

    override fun onImageLoaded() {
        super.onImageLoaded()
        shouldRecenter = true
        invalidate()
    }

}
