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
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class LinearCompassView : BaseCompassView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val formatService = FormatService(context)

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

    private fun drawLocations() {
        val highlighted = _highlightedLocation
        var containsHighlighted = false
        _locations.forEach {
            if (it.id == highlighted?.id) {
                containsHighlighted = true
            }
            drawLocation(
                it,
                highlighted == null || it.id == highlighted.id
            )
        }

        if (highlighted != null && !containsHighlighted) {
            drawLocation(highlighted, true)
        }
    }

    private fun drawLocation(location: IMappableLocation, highlight: Boolean) {
        val bearing = if (_useTrueNorth) {
            _location.bearingTo(location.coordinate)
        } else {
            DeclinationUtils.fromTrueNorthBearing(
                _location.bearingTo(location.coordinate),
                _declination
            )
        }
        val opacity = if (highlight) {
            1f
        } else {
            0.5f
        }
        drawReference(
            MappableReferencePoint(
                location.id,
                R.drawable.ic_arrow_target,
                bearing,
                location.color,
                opacity = opacity
            )
        )
    }

    private fun drawReferences() {
        for (reference in _references) {
            drawReference(reference)
        }
    }

    private fun drawReference(reference: IMappableReferencePoint) {
        val minDegrees = (azimuth.value - range / 2).roundToInt()
        val maxDegrees = (azimuth.value + range / 2).roundToInt()
        val tint = reference.tint
        if (tint != null) {
            tint(tint)
        } else {
            noTint()
        }
        val delta = deltaAngle(
            azimuth.value.roundToInt().toFloat(),
            reference.bearing.value.roundToInt().toFloat()
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
        val bitmap = getBitmap(reference.drawableId, iconSize)
        imageMode(ImageMode.Corner)
        image(
            bitmap, centerPixel - iconSize / 2f,
            0f
        )
        noTint()
        opacity(255)
    }


    private fun drawCompass() {
        val pixDeg = width / range
        val minDegrees = (azimuth.value - range / 2).roundToInt()
        val maxDegrees = (azimuth.value + range / 2).roundToInt()
        var i = -180
        while (i < 540) {
            if (i in minDegrees..maxDegrees) {
                when {
                    i % 45 == 0 -> {
                        noFill()
                        stroke(Resources.color(context, R.color.orange_40))
                        strokeWeight(8f)
                    }
                    else -> {
                        stroke(Resources.androidTextColorPrimary(context))
                        strokeWeight(8f)
                    }
                }
                when {
                    i % 90 == 0 -> {
                        line(
                            pixDeg * (i - minDegrees),
                            height.toFloat(),
                            pixDeg * (i - minDegrees),
                            0.5f * height,
                        )
                        val coord = when (i) {
                            -90, 270 -> west
                            0, 360 -> north
                            90, 450 -> east
                            -180, 180 -> south
                            else -> ""
                        }
                        noStroke()
                        fill(Resources.androidTextColorPrimary(context))
                        textMode(TextMode.Corner)
                        text(coord, pixDeg * (i - minDegrees), 5 / 12f * height)
                    }
                    i % 15 == 0 -> {
                        line(
                            pixDeg * (i - minDegrees),
                            height.toFloat(),
                            pixDeg * (i - minDegrees),
                            0.75f * height
                        )
                    }
                    else -> {
                        line(
                            pixDeg * (i - minDegrees),
                            height.toFloat(),
                            pixDeg * (i - minDegrees),
                            10 / 12f * height
                        )
                    }
                }
            }
            i += 5
        }
        noStroke()
    }

    private fun drawDestination() {
        val d = _destination
        d ?: return
        val delta = deltaAngle(
            azimuth.value.roundToInt().toFloat(),
            d.bearing.value.roundToInt().toFloat()
        )

        val pixelsPerDegree = width / range
        fill(d.color)
        opacity(100)
        rect(width / 2f, height - 0.5f * height, delta * pixelsPerDegree, height * 0.5f)
        opacity(255)
        drawReference(MappableReferencePoint(-1, R.drawable.ic_arrow_target, d.bearing, d.color))
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
        drawReferences()
        drawLocations()
        drawDestination()
    }
}