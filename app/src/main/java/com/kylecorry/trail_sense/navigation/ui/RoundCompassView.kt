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
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
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

    private fun drawDestination() {
        val d = _destination
        d ?: return
        push()
        fill(d.color)
        opacity(100)
        val dp2 = dp(2f)
        arc(
            iconSize.toFloat() + dp2,
            iconSize.toFloat() + dp2,
            compassSize.toFloat(),
            compassSize.toFloat(),
            azimuth.value - 90,
            azimuth.value - 90 + deltaAngle(azimuth.value, d.bearing.value),
            ArcMode.Pie
        )
        opacity(255)
        pop()
    }

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

    private fun drawReferences() {
        for (reference in _references) {
            drawReference(reference)
        }
    }

    private fun drawReference(reference: IMappableReferencePoint, size: Int = iconSize) {
        val tint = reference.tint
        if (tint != null) {
            tint(tint)
        } else {
            noTint()
        }
        opacity((255 * reference.opacity).toInt())
        push()
        rotate(reference.bearing.value)
        val bitmap = getBitmap(reference.drawableId, size)
        imageMode(ImageMode.Corner)
        image(bitmap, width / 2f - size / 2f, (iconSize - size) * 0.6f)
        pop()
        noTint()
        opacity(255)
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

        location.icon?.let { icon ->
            drawReference(
                MappableReferencePoint(
                    location.id,
                    icon.icon,
                    bearing,
                    Colors.mostContrastingColor(Color.WHITE, Color.BLACK, location.color),
                    opacity = opacity
                ),
                (iconSize * 0.35f).toInt()
            )
        }

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
        drawReferences()
        drawLocations()
        drawDestination()
        pop()
    }
}