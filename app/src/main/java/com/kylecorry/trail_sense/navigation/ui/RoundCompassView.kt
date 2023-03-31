package com.kylecorry.trail_sense.navigation.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import com.kylecorry.andromeda.canvas.ArcMode
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.min


class RoundCompassView : BaseCompassView {
    private lateinit var dial: CompassDial

    private var iconSize = 0
    private var compassSize = 0

    private val formatService by lazy { FormatService.getInstance(context) }
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
        opacity(255)
        imageMode(ImageMode.Center)
        dial.draw(drawer)

        noStroke()
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
        fill(Color.WHITE)
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

    override fun setup() {
        super.setup()
        iconSize = dp(24f).toInt()
        compassSize = min(height, width) - 2 * iconSize - 2 * dp(2f).toInt()
        cardinalSize = sp(18f)
        primaryColor = Resources.color(context, R.color.orange_40)
        val secondaryColor = Resources.color(context, R.color.colorSecondary)
        dial = CompassDial(
            PixelCoordinate(width / 2f, height / 2f),
            compassSize / 2f,
            secondaryColor,
            Color.WHITE,
            primaryColor
        )
    }

    override fun draw() {
        if (!isVisible) {
            return
        }
        clear()
        drawAzimuth()
        push()
        rotate(-azimuth.value)
        drawCompass()
        drawCompassLayers()
        pop()
    }

    override fun draw(reference: IMappableReferencePoint, size: Int?) {
        val sizeDp = size?.let { dp(it.toFloat()).toInt() } ?: iconSize
        val tint = reference.tint
        if (tint != null) {
            tint(tint)
        } else {
            noTint()
        }
        opacity((255 * reference.opacity).toInt())
        push()
        rotate(reference.bearing.value)
        val bitmap = getBitmap(reference.drawableId, sizeDp)
        imageMode(ImageMode.Corner)
        image(bitmap, width / 2f - sizeDp / 2f, (iconSize - sizeDp) * 0.6f)
        pop()
        noTint()
        opacity(255)
    }

    override fun draw(bearing: IMappableBearing) {
        push()
        fill(bearing.color)
        opacity(100)
        val dp2 = dp(2f)
        arc(
            iconSize.toFloat() + dp2,
            iconSize.toFloat() + dp2,
            compassSize.toFloat(),
            compassSize.toFloat(),
            azimuth.value - 90,
            azimuth.value - 90 + deltaAngle(azimuth.value, bearing.bearing.value),
            ArcMode.Pie
        )
        opacity(255)
        pop()
    }
}