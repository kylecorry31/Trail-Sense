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
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import kotlin.math.min


class RoundCompassView : CanvasView, INearbyCompassView {
    private val icons = mutableMapOf<Int, Bitmap>()
    private var references = emptyList<IMappableReferencePoint>()
    private var compass: Bitmap? = null
    private var azimuth = 0f
    private var locations: List<IMappableLocation> = emptyList()
    private var location: Coordinate = Coordinate.zero
    private var declination: Float = 0f
    private var destination: IMappableBearing? = null

    private var iconSize = 0
    private var compassSize = 0

    private val formatService by lazy { FormatService(context) }
    private val north by lazy { formatService.formatDirection(CompassDirection.North) }
    private val south by lazy { formatService.formatDirection(CompassDirection.South) }
    private val east by lazy { formatService.formatDirection(CompassDirection.East) }
    private val west by lazy { formatService.formatDirection(CompassDirection.West) }
    private val prefs by lazy { UserPreferences(context) }
    private var useTrueNorth = false
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
        push()
        fill(d.color)
        opacity(100)
        val dp2 = dp(2f)
        arc(
            iconSize.toFloat() + dp2,
            iconSize.toFloat() + dp2,
            compassSize.toFloat(),
            compassSize.toFloat(),
            azimuth - 90,
            azimuth - 90 + deltaAngle(azimuth, d.bearing.value),
            ArcMode.Pie
        )
        opacity(255)
        pop()
    }

    override fun setAzimuth(azimuth: Bearing) {
        this.azimuth = azimuth.value
        invalidate()
    }

    override fun setLocation(location: Coordinate) {
        this.location = location
        invalidate()
    }

    private fun drawAzimuth() {
        tint(Resources.androidTextColorPrimary(context))
        imageMode(ImageMode.Corner)
        image(
            getBitmap(R.drawable.ic_arrow_target),
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

    private fun drawReferences() {
        for (reference in references) {
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
        val bitmap = getBitmap(reference.drawableId)
        imageMode(ImageMode.Corner)
        image(bitmap, width / 2f - iconSize / 2f, 0f)
        pop()
        noTint()
        opacity(255)
    }

    override fun setDeclination(declination: Float) {
        this.declination = declination
        invalidate()
    }

    override fun showLocations(locations: List<IMappableLocation>) {
        this.locations = locations
        invalidate()
    }

    override fun showPaths(paths: List<IMappablePath>) {
        // Do nothing
    }

    override fun showReferences(references: List<IMappableReferencePoint>) {
        this.references = references
        invalidate()
    }

    override fun showDirection(bearing: IMappableBearing?) {
        destination = bearing
        invalidate()
    }

    private fun drawLocations() {
        locations.forEach { drawLocation(it) }
    }

    private fun drawLocation(location: IMappableLocation) {
        val bearing = if (useTrueNorth) {
            this.location.bearingTo(location.coordinate)
        } else {
            DeclinationUtils.fromTrueNorthBearing(
                this.location.bearingTo(location.coordinate),
                declination
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
        useTrueNorth = prefs.navigation.useTrueNorth
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
        drawReferences()
        drawLocations()
        drawDestination()
        pop()
    }
}