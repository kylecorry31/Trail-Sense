package com.kylecorry.trail_sense.tools.experimentation

import android.content.Context
import android.graphics.Color
import android.graphics.Path
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
    var sideInclination = 0f

    var points = listOf<Point>()

    override fun setup() {
    }

    override fun draw() {
        push()
        // TODO: Come up with a better way to do this
        rotate(sideInclination)
        clear()

        val horizonPath = Path()
        for (i in 0..360 step 5){
            val pixel = getPixel(i.toFloat(), 0f)
            if (i == 0){
                horizonPath.moveTo(pixel.x, pixel.y)
            } else {
                horizonPath.lineTo(pixel.x, pixel.y)
            }
        }
        horizonPath.close()

        noFill()
        stroke(Color.WHITE)
        strokeWeight(2f)
        path(horizonPath)

        noStroke()


        points.forEach {
            val pixel = getPixel(it.bearing, it.elevation)
            fill(it.color)
            circle(pixel.x, pixel.y, getSize(it.size))
        }
        pop()
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

    data class Point(val bearing: Float, val elevation: Float, val size: Float, val color: Int)

}