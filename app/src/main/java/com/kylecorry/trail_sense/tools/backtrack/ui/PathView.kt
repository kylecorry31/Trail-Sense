package com.kylecorry.trail_sense.tools.backtrack.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.ArrowPathEffect
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.DottedPathEffect
import com.kylecorry.andromeda.core.math.cosDegrees
import com.kylecorry.andromeda.core.math.deltaAngle
import com.kylecorry.andromeda.core.math.sinDegrees
import com.kylecorry.andromeda.core.math.wrap
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.toPixelLines
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trailsensecore.domain.geo.PathStyle
import com.kylecorry.trailsensecore.domain.geo.cartography.MapRegion
import com.kylecorry.trailsensecore.domain.pixels.PixelLine
import com.kylecorry.trailsensecore.domain.pixels.PixelLineStyle
import kotlin.math.max

class PathView(context: Context, attrs: AttributeSet? = null) : CanvasView(context, attrs) {

    var path: List<WaypointEntity> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var location: Coordinate? = null
        set(value) {
            field = value
            invalidate()
        }

    var azimuth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val prefs by lazy { UserPreferences(context) }
    private val formatService by lazy { FormatServiceV2(context) }
    private val pathColor by lazy { prefs.navigation.backtrackPathColor }
    private val pathStyle by lazy { prefs.navigation.backtrackPathStyle }

    init {
        runEveryCycle = false
    }

    override fun setup() {
    }

    override fun draw() {
        clear()
        drawMap()
    }

    private fun drawMap() {
        val bounds = getPathBounds(path.map { it.coordinate }) ?: return
        val distanceX = bounds.southEast.distanceTo(bounds.southWest)
        val distanceY = bounds.northWest.distanceTo(bounds.southWest)

        val h = height.toFloat() - dp(32f)
        val w = width.toFloat() - dp(32f)

        val metersPerPixel = max(distanceY / h, distanceX / w)

        val gridGap = getGridSize(Distance.meters(distanceX))
        drawGrid(metersPerPixel, gridGap.meters().distance)
        drawLegend(gridGap)

        val pathLines =
            path.map { it.coordinate }.toPixelLines(pathColor.color, mapPixelLineStyle(pathStyle)) {
                getPixels(bounds.center, metersPerPixel, it)
            }
        drawPaths(pathLines)
        location?.let {
            drawLocation(getPixels(bounds.center, metersPerPixel, it))
        }
    }

    private fun getGridSize(distance: Distance): Distance {
        val baseUnits = prefs.baseDistanceUnits
        return if (baseUnits == DistanceUnits.Meters) {
            Distance.meters(if (distance.meters().distance < 500f) 10f else 100f)
        } else {
            Distance.feet(
                if (distance.convertTo(baseUnits).distance < 500f) {
                    30f
                } else {
                    300f
                }
            )
        }
    }

    private fun drawLegend(gridGap: Distance) {
        textMode(TextMode.Corner)
        textSize(sp(14f))
        strokeWeight(0f)
        fill(Color.WHITE)
        val distanceText = context.getString(
            R.string.grid_size,
            formatService.formatDistance(gridGap)
        )
        val textWidth = textWidth(distanceText)
        text(distanceText, width - textWidth - dp(16f), height.toFloat() - dp(16f))
    }

    private fun drawGrid(
        metersPerPixel: Float,
        gap: Float,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ) {
        noFill()
        stroke(Color.WHITE)
        strokeWeight(dp(0.5f))
        opacity(50)
        // Vertical
        var i = offsetX / metersPerPixel
        while (i < width) {
            line(i, 0f, i, height.toFloat())
            i += gap / metersPerPixel
        }

        // Horizontal
        i = offsetY / metersPerPixel
        while (i < height) {
            line(0f, i, width.toFloat(), i)
            i += gap / metersPerPixel
        }

        opacity(255)
    }

    private fun drawLocation(pixels: PixelCoordinate) {
        stroke(Color.WHITE)
        strokeWeight(dp(1f))
        fill(Resources.color(context, R.color.colorPrimary))
        push()
        rotate(azimuth, pixels.x, pixels.y)
        triangle(
            pixels.x, pixels.y - dp(6f),
            pixels.x - dp(5f), pixels.y + dp(6f),
            pixels.x + dp(5f), pixels.y + dp(6f)
        )
        pop()
    }

    private fun drawPaths(pathLines: List<PixelLine>) {
        val dotted = DottedPathEffect(3f, 10f)
        val arrow = ArrowPathEffect(6f)
        clear()
        for (line in pathLines) {
            when (line.style) {
                PixelLineStyle.Solid -> {
                    noPathEffect()
                    noFill()
                    stroke(line.color)
                    strokeWeight(6f)
                }
                PixelLineStyle.Arrow -> {
                    pathEffect(arrow)
                    noStroke()
                    fill(line.color)
                }
                PixelLineStyle.Dotted -> {
                    pathEffect(dotted)
                    noStroke()
                    fill(line.color)
                }
            }
            opacity(line.alpha)
            val xOffset = 0f
            val yOffset = 0f
            line(
                line.start.x - xOffset,
                line.start.y - yOffset,
                line.end.x - xOffset,
                line.end.y - yOffset
            )
            opacity(255)
            noStroke()
            fill(Color.WHITE)
            noPathEffect()
        }
    }

    private fun getPixels(
        center: Coordinate,
        metersPerPixel: Float,
        location: Coordinate
    ): PixelCoordinate {
        val distance = center.distanceTo(location)
        val bearing = center.bearingTo(location)
        val angle = wrap(-(bearing.value - 90), 0f, 360f)
        val pixelDistance = distance / metersPerPixel
        val xDiff = cosDegrees(angle.toDouble()).toFloat() * pixelDistance
        val yDiff = sinDegrees(angle.toDouble()).toFloat() * pixelDistance
        return PixelCoordinate(width / 2f + xDiff, height / 2f - yDiff)
    }

    private fun getPathBounds(locations: List<Coordinate>): MapRegion? {
        val west = getWestLongitudeBound(locations) ?: return null
        val east = getEastLongitudeBound(locations) ?: return null
        val north = getNorthLatitudeBound(locations) ?: return null
        val south = getSouthLatitudeBound(locations) ?: return null
        return MapRegion(north, east, south, west)
    }

    private fun getWestLongitudeBound(locations: List<Coordinate>): Double? {
        val first = locations.firstOrNull() ?: return null
        return locations.minByOrNull {
            deltaAngle(
                first.longitude.toFloat() + 180,
                it.longitude.toFloat() + 180
            )
        }?.longitude
    }

    private fun getEastLongitudeBound(locations: List<Coordinate>): Double? {
        val first = locations.firstOrNull() ?: return null
        return locations.maxByOrNull {
            deltaAngle(
                first.longitude.toFloat() + 180,
                it.longitude.toFloat() + 180
            )
        }?.longitude
    }

    private fun getSouthLatitudeBound(locations: List<Coordinate>): Double? {
        return locations.minByOrNull { it.latitude }?.latitude
    }

    private fun getNorthLatitudeBound(locations: List<Coordinate>): Double? {
        return locations.maxByOrNull { it.latitude }?.latitude
    }

    private fun mapPixelLineStyle(style: PathStyle): PixelLineStyle {
        return when (style) {
            PathStyle.Solid -> PixelLineStyle.Solid
            PathStyle.Dotted -> PixelLineStyle.Dotted
            PathStyle.Arrow -> PixelLineStyle.Arrow
        }
    }

}