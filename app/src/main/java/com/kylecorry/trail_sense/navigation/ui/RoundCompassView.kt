package com.kylecorry.trail_sense.navigation.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.min


class RoundCompassView : CanvasView, ICompassView {
    private val icons = mutableMapOf<Int, Bitmap>()
    private var indicators = listOf<BearingIndicator>()
    private var compass: Bitmap? = null
    private var azimuth = 0f
    private var destination: Float? = null
    @ColorInt
    private var destinationColor: Int? = null

    private var iconSize = 0
    private var compassSize = 0

    private val formatService by lazy { FormatService(context) }
    private val north by lazy { formatService.formatDirection(CompassDirection.North) }
    private val south by lazy { formatService.formatDirection(CompassDirection.South) }
    private val east by lazy { formatService.formatDirection(CompassDirection.East) }
    private val west by lazy { formatService.formatDirection(CompassDirection.West) }
    private var cardinalSize = 0f

    @ColorInt
    private var primaryColor = Color.WHITE

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

    private fun drawDestination() {
        val d = destination
        d ?: return
        val color = destinationColor ?: primaryColor
        push()
        fill(color)
        opacity(100)
        val dp2 = dp(2f)
        arc(
            iconSize.toFloat() + dp2,
            iconSize.toFloat() + dp2,
            compassSize.toFloat(),
            compassSize.toFloat(),
            azimuth - 90,
            azimuth - 90 + deltaAngle(azimuth, d),
            ArcMode.Pie
        )
        opacity(255)
        pop()
    }

    override fun setAzimuth(azimuth: Float) {
        this.azimuth = azimuth
        invalidate()
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

    private fun drawAzimuth() {
        tint(Resources.androidTextColorPrimary(context))
        imageMode(ImageMode.Corner)
        image(getBitmap(R.drawable.ic_arrow_target),
            width / 2f - iconSize / 2f,
            0f)
        noTint()
    }

    private fun drawCompass() {
        opacity(255)
        imageMode(ImageMode.Center)
        image(compass!!, width / 2f, height / 2f)

        fill(Color.WHITE)
        circle(width / 2f, height / 2f, dp(16f))


        stroke(color(100))
        noFill()
        strokeWeight(3f)
        circle(width / 2f, height / 2f, compassSize / 2f)


        textSize(cardinalSize)
        textMode(TextMode.Center)
        stroke(Resources.color(context, R.color.colorSecondary))
        strokeWeight(32f)
        push()
        rotate(0f)
        fill(primaryColor)
        text(
            north,
            width / 2f,
            height / 2f - compassSize / 4f
        )
        pop()

        push()
        rotate(180f)
        fill(Color.WHITE)
        text(
            south,
            width / 2f,
            height / 2f - compassSize / 4f
        )
        pop()

        push()
        rotate(90f)
        fill(Color.WHITE)
        text(
            east,
            width / 2f,
            height / 2f - compassSize / 4f
        )
        pop()

        push()
        rotate(270f)
        fill(Color.WHITE)
        text(
            west,
            width / 2f,
            height / 2f - compassSize / 4f
        )
        pop()

        noStroke()
    }

    private fun drawBearings() {
        for (indicator in indicators) {
            if (indicator.tint != null){
                tint(indicator.tint)
            } else {
                noTint()
            }
            opacity((255 * indicator.opacity).toInt())
            push()
            rotate(indicator.bearing)
            val bitmap = getBitmap(indicator.icon)
            imageMode(ImageMode.Corner)
            image(bitmap, width / 2f - iconSize / 2f, 0f)
            pop()
        }
        noTint()
        opacity(255)
    }

    override fun setDeclination(declination: Float) {
        // Do nothing for now
    }

    private fun getBitmap(@DrawableRes id: Int): Bitmap {
        val bitmap = if (icons.containsKey(id)) {
            icons[id]
        } else {
            val drawable = Resources.drawable(context, id)
            val bm = drawable?.toBitmap(iconSize, iconSize)
            icons[id] = bm!!
            icons[id]
        }
        return bitmap!!
    }

    override fun setup() {
        iconSize = dp(24f).toInt()
        compassSize = min(height, width) - 2 * iconSize - 2 * dp(2f).toInt()
        compass = loadImage(R.drawable.compass, compassSize, compassSize)
        cardinalSize = sp(18f)
        primaryColor = Resources.color(context, R.color.colorPrimary)
    }

    override fun draw() {
        if (!isVisible) {
            return
        }
        clear()
        drawAzimuth()
        push()
        rotate(-azimuth)
        drawCompass()
        drawBearings()
        drawDestination()
        pop()
    }
}