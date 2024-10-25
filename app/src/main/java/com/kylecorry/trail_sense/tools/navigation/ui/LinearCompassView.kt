package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.shared.extensions.getValuesBetween

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

    private val rawMinimum: Float
        get() = azimuth.value - range / 2

    private val rawMaximum: Float
        get() = azimuth.value + range / 2

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
        val values = getValuesBetween(rawMinimum, rawMaximum, 5f).map { it.toInt() }

        val primaryColor = Resources.getCardinalDirectionColor(context)
        val secondaryColor = Resources.androidTextColorPrimary(context)

        values.forEach {
            val x = toPixel(it.toFloat())

            // Set the color
            if (it % 45 == 0) {
                stroke(primaryColor)
                strokeWeight(8f)
            } else {
                stroke(secondaryColor)
                strokeWeight(8f)
            }

            val lineHeight = when {
                it % 90 == 0 -> 0.5f * height
                it % 15 == 0 -> 0.75f * height
                else -> 10 / 12f * height
            }

            // Draw the line
            line(
                x,
                height.toFloat(),
                x,
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
                fill(secondaryColor)
                textMode(TextMode.Corner)
                text(coord, x, 5 / 12f * height)
            }
        }

        noStroke()
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
        val tint = reference.tint
        if (tint != null) {
            tint(tint)
        } else {
            noTint()
        }
        val x = toPixel(reference.bearing.value).coerceIn(0f, width.toFloat())
        opacity((255 * reference.opacity).toInt())
        val bitmap = getBitmap(reference.drawableId, sizeDp)
        imageMode(ImageMode.Corner)
        push()
        translate(x - sizeDp / 2f, (iconSize - sizeDp) * 0.6f)
        rotate(reference.rotation, bitmap.width / 2f, bitmap.height / 2f)
        image(bitmap, 0f, 0f)
        pop()
        noTint()
        opacity(255)

    }

    override fun draw(bearing: IMappableBearing, stopAt: Coordinate?) {
        val x = toPixel(bearing.bearing.value) - width / 2f
        fill(bearing.color)
        opacity(100)
        rect(width / 2f, height - 0.5f * height, x, height * 0.5f)
        opacity(255)
    }

    private fun toPixel(bearing: Float): Float {
        return AugmentedRealityUtils.getPixelLinear(
            bearing,
            azimuth.value,
            0f,
            0f,
            Size(width.toFloat(), height.toFloat()),
            Size(range, 0f)
        ).x
    }
}