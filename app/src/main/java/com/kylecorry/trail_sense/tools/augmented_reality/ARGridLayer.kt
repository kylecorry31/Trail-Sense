package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundNearest
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.getValuesBetween
import kotlin.math.absoluteValue
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

class ARGridLayer(
    private val spacing: Int = 30,
    @ColorInt private val color: Int = Color.WHITE,
    @ColorInt private val northColor: Int = color,
    @ColorInt private val horizonColor: Int = color,
    @ColorInt private val labelColor: Int = color,
    private val thicknessDp: Float = 1f,
    private val resolutionDegrees: Int = 5
) : ARLayer {

    private var isSetup = false
    private var textSize: Float = 0f
    private var northString: String = ""
    private var southString: String = ""
    private var eastString: String = ""
    private var westString: String = ""

    private val path = Path()

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {

        if (!isSetup){
            textSize = drawer.sp(16f)
            northString = view.context.getString(R.string.direction_north)
            southString = view.context.getString(R.string.direction_south)
            eastString = view.context.getString(R.string.direction_east)
            westString = view.context.getString(R.string.direction_west)
            isSetup = true
        }

        val maxAngle = hypot(view.fov.width, view.fov.height) * 1.5f

        val resolutionDegrees = (maxAngle / 10f).roundToInt().coerceIn(1, 5)

        val minVertical = (view.inclination - maxAngle / 2f).toInt().coerceIn(-90, 90)
        val maxVertical = (view.inclination + maxAngle / 2f).toInt().coerceIn(-90, 90)

        val isPoleVisible = minVertical.absoluteValue == 90 || maxVertical.absoluteValue == 90

        val minHorizontal = if (isPoleVisible) 0 else (view.azimuth - maxAngle / 2f).toInt()
        val maxHorizontal = if (isPoleVisible) 360 else (view.azimuth + maxAngle / 2f).toInt()

        val latitudes = getValuesBetween(minVertical.toFloat(), maxVertical.toFloat(), spacing.toFloat())
        val longitudes = getValuesBetween(minHorizontal.toFloat(), maxHorizontal.toFloat(), spacing.toFloat()).distinctBy {
            SolMath.normalizeAngle(it)
        }

        drawer.noFill()
        drawer.strokeWeight(drawer.dp(thicknessDp))

        val maxDistance = min(view.width, view.height)

        // Draw horizontal lines
        val horizontalPointRange = steppedRangeInclusive(minHorizontal, maxHorizontal, resolutionDegrees)
        for (i in latitudes) {
            path.reset()
            if (i.toInt() == 0){
                drawer.stroke(horizonColor)
            } else {
                drawer.stroke(color)
            }
            var previous: PixelCoordinate? = null
            for (j in horizontalPointRange) {
                val pixel = view.toPixel(AugmentedRealityView.HorizonCoordinate(j.toFloat(), i))
                if (previous != null && pixel.distanceTo(previous) < maxDistance){
                    path.lineTo(pixel.x, pixel.y)
                } else {
                    path.moveTo(pixel.x, pixel.y)
                }
                previous = pixel
            }
            drawer.path(path)
        }

        // Draw vertical lines
        val verticalPointRange = steppedRangeInclusive(minVertical, maxVertical, resolutionDegrees)
        for (i in longitudes) {
            path.reset()
            if (i.toInt() == 0){
                drawer.stroke(northColor)
            } else {
                drawer.stroke(color)
            }
            var previous: PixelCoordinate? = null
            for (j in verticalPointRange) {
                val pixel = view.toPixel(AugmentedRealityView.HorizonCoordinate(i, j.toFloat()))
                if (previous != null && pixel.distanceTo(previous) < maxDistance){
                    path.lineTo(pixel.x, pixel.y)
                } else {
                    path.moveTo(pixel.x, pixel.y)
                }
                previous = pixel
            }
            drawer.path(path)
        }


        drawer.noStroke()

        // Draw cardinal direction labels
        val offset = 2f
        val north = view.toPixel(AugmentedRealityView.HorizonCoordinate(0f, offset))
        val south = view.toPixel(AugmentedRealityView.HorizonCoordinate(180f, offset))
        val east = view.toPixel(AugmentedRealityView.HorizonCoordinate(90f, offset))
        val west = view.toPixel(AugmentedRealityView.HorizonCoordinate(-90f, offset))

        drawLabel(drawer, view, northString, north)
        drawLabel(drawer, view, southString, south)
        drawLabel(drawer, view, eastString, east)
        drawLabel(drawer, view, westString, west)
    }

    private fun drawLabel(drawer: ICanvasDrawer, view: AugmentedRealityView, text: String, position: PixelCoordinate){
        drawer.textSize(drawer.sp(16f))
        drawer.fill(labelColor)
        drawer.push()
        drawer.rotate(view.sideInclination, position.x, position.y)
        drawer.text(text, position.x, position.y)
        drawer.pop()
    }

    private fun steppedRangeInclusive(min: Int, max: Int, step: Int): List<Int> {
        val values = mutableListOf<Int>()
        for (i in min..max step step) {
            values.add(i)
        }
        if (values.lastOrNull() != max){
            values.add(max)
        }
        return values
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        return false
    }
}