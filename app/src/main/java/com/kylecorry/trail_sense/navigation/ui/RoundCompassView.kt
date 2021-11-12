package com.kylecorry.trail_sense.navigation.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import kotlin.math.min


class RoundCompassView : BaseCompassView {
    private var compass: Bitmap? = null

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
            _azimuth - 90,
            _azimuth - 90 + deltaAngle(_azimuth, d.bearing.value),
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
        image(compass!!, width / 2f, height / 2f)

        fill(Color.WHITE)
        circle(width / 2f, height / 2f, dp(16f))


        stroke(color(100))
        noFill()
        strokeWeight(3f)
        circle(width / 2f, height / 2f, compassSize / 2f)


        textSize(cardinalSize)
        textMode(TextMode.Center)
        stroke(Resources.getAndroidColorAttr(context, R.attr.colorSecondary))
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

    private fun drawReferences() {
        for (reference in _references) {
            drawReference(reference)
        }
    }

    private fun drawReference(reference: IMappableReferencePoint) {
        val tint = reference.tint
        if (tint != null) {
            tint(tint)
        } else {
            noTint()
        }
        opacity((255 * reference.opacity).toInt())
        push()
        rotate(reference.bearing.value)
        val bitmap = getBitmap(reference.drawableId, iconSize)
        imageMode(ImageMode.Corner)
        image(bitmap, width / 2f - iconSize / 2f, 0f)
        pop()
        noTint()
        opacity(255)
    }

    private fun drawLocations() {
        _locations.forEach { drawLocation(it) }
    }

    private fun drawLocation(location: IMappableLocation) {
        val bearing = if (_useTrueNorth) {
            _location.bearingTo(location.coordinate)
        } else {
            DeclinationUtils.fromTrueNorthBearing(
                _location.bearingTo(location.coordinate),
                _declination
            )
        }
        drawReference(
            MappableReferencePoint(
                location.id,
                R.drawable.ic_arrow_target,
                bearing,
                location.color
            )
        )
    }

    override fun finalize() {
        super.finalize()
        tryOrNothing {
            compass?.recycle()
        }
    }

    override fun setup() {
        super.setup()
        iconSize = dp(24f).toInt()
        compassSize = min(height, width) - 2 * iconSize - 2 * dp(2f).toInt()
        compass = loadImage(R.drawable.compass, compassSize, compassSize)
        cardinalSize = sp(18f)
        primaryColor = Resources.getAndroidColorAttr(context, R.attr.colorPrimary)
    }

    override fun draw() {
        if (!isVisible) {
            return
        }
        clear()
        drawAzimuth()
        push()
        rotate(-_azimuth)
        drawCompass()
        drawReferences()
        drawLocations()
        drawDestination()
        pop()
    }
}