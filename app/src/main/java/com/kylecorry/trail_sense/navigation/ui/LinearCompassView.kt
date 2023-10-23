package com.kylecorry.trail_sense.navigation.ui

/**
 * Adapted from https://github.com/RedInput/CompassView
 *
 * View original license: https://github.com/RedInput/CompassView/blob/master/LICENSE
 */

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.math.SolMath.roundNearest
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.absoluteValue

class LinearCompassView : BaseCompassView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val formatService = FormatService.getInstance(context)

    private val north by lazy { formatService.formatDirection(CompassDirection.North) }
    private val south by lazy { formatService.formatDirection(CompassDirection.South) }
    private val east by lazy { formatService.formatDirection(CompassDirection.East) }
    private val west by lazy { formatService.formatDirection(CompassDirection.West) }

    var range = 180f

    private var iconSize = 0
    private var textSize = 0f

    private fun drawAzimuth() {
        tint(Resources.androidTextColorPrimary(context))
        imageMode(ImageMode.Corner)
        image(
            getBitmap(R.drawable.ic_arrow_target, iconSize),
            width / 2f - iconSize / 2f,
            0f
        )
        noTint()
    }


    private fun drawCompass() {
        val pixDeg = width / range

        val mn = azimuth.value - range / 2
        val mx = azimuth.value + range / 2

        val values = getValuesBetween(mn, mx, 5f).map { it.toInt() }

        values.forEach {
            // Set the color
            if (it % 45 == 0) {
                stroke(Resources.color(context, R.color.orange_40))
                strokeWeight(8f)
            } else {
                stroke(Resources.androidTextColorPrimary(context))
                strokeWeight(8f)
            }

            val lineHeight = when {
                it % 90 == 0 -> 0.5f * height
                it % 15 == 0 -> 0.75f * height
                else -> 10 / 12f * height
            }

            // Draw the line
            line(
                pixDeg * (it - mn),
                height.toFloat(),
                pixDeg * (it - mn),
                lineHeight,
            )

            // Draw the label
            if (it % 90 == 0) {
                val coord = when (it) {
                    -90, 270 -> west
                    0, 360 -> north
                    90, 450 -> east
                    -180, 180 -> south
                    else -> ""
                }
                noStroke()
                fill(Resources.androidTextColorPrimary(context))
                textMode(TextMode.Corner)
                text(coord, pixDeg * (it - mn), 5 / 12f * height)
            }
        }

        noStroke()
    }

    /**
     * Returns the values between min and max, inclusive, that are divisible by divisor
     * @param min The minimum value
     * @param max The maximum value
     * @param divisor The divisor
     * @return The values between min and max, inclusive, that are divisible by divisor
     */
    private fun getValuesBetween(min: Float, max: Float, divisor: Float): List<Float> {
        val values = mutableListOf<Float>()
        val start = min.roundNearest(divisor)
        var i = start
        while (i <= max) {
            if (i >= min) {
                values.add(i)
            }
            i += divisor
        }
        return values
    }

    override fun setup() {
        super.setup()
        textAlign(TextAlign.Center)
        iconSize = dp(25f).toInt()
        textSize = sp(15f)
        textSize(textSize)
    }

    override fun draw() {
        if (!isVisible) {
            return
        }
        clear()
        drawAzimuth()
        drawCompass()
        drawCompassLayers()
    }

    override fun draw(reference: IMappableReferencePoint, size: Int?) {
        val sizeDp = size?.let { dp(it.toFloat()).toInt() } ?: iconSize

        val minDegrees = azimuth.value - range / 2
        val maxDegrees = azimuth.value + range / 2
        val tint = reference.tint
        if (tint != null) {
            tint(tint)
        } else {
            noTint()
        }
        val delta = deltaAngle(
            azimuth.value,
            reference.bearing.value
        )
        val centerPixel = when {
            delta < -range / 2f -> {
                0f // TODO: Display indicator that is off screen
            }

            delta > range / 2f -> {
                width.toFloat() // TODO: Display indicator that is off screen
            }

            else -> {
                val deltaMin = deltaAngle(
                    reference.bearing.value,
                    minDegrees.toFloat()
                ).absoluteValue / (maxDegrees - minDegrees).toFloat()
                deltaMin * width
            }
        }
        opacity((255 * reference.opacity).toInt())
        val bitmap = getBitmap(reference.drawableId, sizeDp)
        imageMode(ImageMode.Corner)
        image(
            bitmap, centerPixel - sizeDp / 2f,
            (iconSize - sizeDp) * 0.6f
        )
        noTint()
        opacity(255)

    }

    override fun draw(bearing: IMappableBearing, stopAt: Coordinate?) {
        val delta = deltaAngle(
            azimuth.value,
            bearing.bearing.value
        )

        val pixelsPerDegree = width / range
        fill(bearing.color)
        opacity(100)
        rect(width / 2f, height - 0.5f * height, delta * pixelsPerDegree, height * 0.5f)
        opacity(255)
    }
}