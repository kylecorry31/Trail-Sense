package com.kylecorry.trail_sense.navigation.ui

/**
 * Adapted from https://github.com/RedInput/CompassView
 *
 * View original license: https://github.com/RedInput/CompassView/blob/master/LICENSE
 */

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.views.CanvasView
import com.kylecorry.trailsensecore.domain.geo.CompassDirection
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.math.deltaAngle
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class LinearCompassView : CanvasView, ICompassView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val formatService = FormatServiceV2(context)
    private val icons = mutableMapOf<Int, Bitmap>()
    private var indicators = listOf<BearingIndicator>()
    private var azimuth = 0f
    private var destination: Float? = null
    @ColorInt
    private var destinationColor: Int? = null

    private val north by lazy { formatService.formatDirection(CompassDirection.North) }
    private val south by lazy { formatService.formatDirection(CompassDirection.South) }
    private val east by lazy { formatService.formatDirection(CompassDirection.East) }
    private val west by lazy { formatService.formatDirection(CompassDirection.West) }

    var range = 180f

    private var iconSize = 0
    private var textSize = 0f

    init {
        runEveryCycle = false
        setupAfterVisible = true
    }

    fun finalize() {
        try {
            for (icon in icons){
                icon.value.recycle()
            }
            icons.clear()
        } catch (e: Exception) {
        }
    }

    private fun drawBearings() {
        val minDegrees = (azimuth - range / 2).roundToInt()
        val maxDegrees = (azimuth + range / 2).roundToInt()
        for (indicator in indicators) {
            if (indicator.tint != null){
                tint(indicator.tint)
            } else {
                noTint()
            }
            val delta = deltaAngle(
                azimuth.roundToInt().toFloat(),
                indicator.bearing.roundToInt().toFloat()
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
                        indicator.bearing,
                        minDegrees.toFloat()
                    ).absoluteValue / (maxDegrees - minDegrees).toFloat()
                    deltaMin * width
                }
            }
            opacity((255 * indicator.opacity).toInt())
            val bitmap = getBitmap(indicator.icon)
            imageMode(ImageMode.Corner)
            image(bitmap, centerPixel - iconSize / 2f,
                0f
            )
        }
        noTint()
        opacity(255)
    }

    private fun drawAzimuth() {
        tint(UiUtils.androidTextColorPrimary(context))
        imageMode(ImageMode.Corner)
        image(getBitmap(R.drawable.ic_arrow_target),
            width / 2f - iconSize / 2f,
            0f
        )
        noTint()
    }


    private fun drawCompass() {
        val pixDeg = width / range
        val minDegrees = (azimuth - range / 2).roundToInt()
        val maxDegrees = (azimuth + range / 2).roundToInt()
        var i = -180
        while (i < 540) {
            if (i in minDegrees..maxDegrees) {
                when {
                    i % 45 == 0 -> {
                        noFill()
                        stroke(UiUtils.color(context, R.color.colorPrimary))
                        strokeWeight(8f)
                    }
                    else -> {
                        stroke(UiUtils.androidTextColorPrimary(context))
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
                        fill(UiUtils.androidTextColorPrimary(context))
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
        val d = destination
        d ?: return
        val color = destinationColor ?: UiUtils.color(context, R.color.colorPrimary)
        val delta = deltaAngle(
            azimuth.roundToInt().toFloat(),
            d.roundToInt().toFloat()
        )

        val pixelsPerDegree = width / range
        fill(color)
        opacity(100)
        rect(width / 2f, height - 0.5f * height, delta * pixelsPerDegree, height * 0.5f)
        opacity(255)
    }

    private fun getBitmap(@DrawableRes id: Int): Bitmap {
        val bitmap = if (icons.containsKey(id)) {
            icons[id]
        } else {
            val drawable = UiUtils.drawable(context, id)
            val bm = drawable?.toBitmap(iconSize, iconSize)
            icons[id] = bm!!
            icons[id]
        }
        return bitmap!!
    }

    override fun setAzimuth(azimuth: Float) {
        this.azimuth = azimuth
        invalidate()
    }

    override fun setDeclination(declination: Float) {
        // Do nothing for now
    }

    override fun setLocation(location: Coordinate) {
        // Nothing
    }

    override fun setIndicators(indicators: List<BearingIndicator>) {
        this.indicators = indicators
        invalidate()
    }

    override fun setDestination(bearing: Float?, @ColorInt color: Int?) {
        destination = bearing
        destinationColor = color
        invalidate()
    }

    override fun setup() {
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
        drawBearings()
        drawDestination()
    }
}