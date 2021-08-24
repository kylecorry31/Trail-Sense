package com.kylecorry.trail_sense.tools.backtrack.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.ArrowPathEffect
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.DottedPathEffect
import com.kylecorry.andromeda.core.math.cosDegrees
import com.kylecorry.andromeda.core.math.sinDegrees
import com.kylecorry.andromeda.core.math.wrap
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.toPixelLines
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
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
        val dpi = Screen.dpi(context)
        val scale = prefs.navigation.rulerScale
        val realWidth = Distance(scale * w / dpi, DistanceUnits.Inches).meters().distance
        val displayScale = (distanceX / realWidth).toInt()

        textMode(TextMode.Corner)
        textSize(sp(14f))
        fill(Resources.androidTextColorSecondary(context))
        val distanceText = "1 : $displayScale"
        val textHeight = textHeight(distanceText)
        val textWidth = textWidth(distanceText)
        text(distanceText, width - textWidth - dp(8f), height - textHeight + dp(8f))

        val pathLines =
            path.map { it.coordinate }.toPixelLines(pathColor.color, PixelLineStyle.Solid) {
                getPixels(bounds.center, metersPerPixel, it)
            }
        drawPaths(pathLines)
        location?.let {
            drawLocation(getPixels(bounds.center, metersPerPixel, it))
        }
    }

    private fun drawLocation(pixels: PixelCoordinate){
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

    // TODO: This isn't right
    private fun getWestLongitudeBound(locations: List<Coordinate>): Double? {
        return locations.minByOrNull { it.longitude }?.longitude
    }

    // TODO: This isn't right
    private fun getEastLongitudeBound(locations: List<Coordinate>): Double? {
        return locations.maxByOrNull { it.longitude }?.longitude
    }

    private fun getSouthLatitudeBound(locations: List<Coordinate>): Double? {
        return locations.minByOrNull { it.latitude }?.latitude
    }

    private fun getNorthLatitudeBound(locations: List<Coordinate>): Double? {
        return locations.maxByOrNull { it.latitude }?.latitude
    }

}