package com.kylecorry.trail_sense.tools.experimentation

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils

class AugmentedRealityView: CanvasView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var fov: Size = Size(45f, 45f)
    var azimuth = 0f
    var inclination = 0f

    var points = listOf<Triple<Float, Float, Float>>()

    override fun setup() {
    }

    override fun draw() {
        clear()
        fill(Color.WHITE)
        points.forEach {
            val pixel = getPixel(it.first, it.second)
            circle(pixel.x, pixel.y, getSize(it.third))
        }
    }

    private fun getSize(angularSize: Float): Float {
        return (width / fov.width) * angularSize
    }

    private fun getPixel(bearing: Float, elevation: Float): PixelCoordinate {
        return AugmentedRealityUtils.getPixelLinear(
            bearing,
            azimuth,
            elevation,
            inclination,
            Size(width.toFloat(), height.toFloat()),
            fov
        )
    }


}