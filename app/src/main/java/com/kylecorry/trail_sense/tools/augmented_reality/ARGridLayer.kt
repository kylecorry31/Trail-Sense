package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.augmented_reality.position.ARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.position.SphericalARPoint

class ARGridLayer(
    spacing: Int = 30,
    @ColorInt private val color: Int = Color.WHITE,
    @ColorInt private val northColor: Int = color,
    @ColorInt private val horizonColor: Int = color,
    @ColorInt private val labelColor: Int = color,
    thicknessDp: Float = 1f,
    private val useTrueNorth: Boolean = true,
) : ARLayer {

    private val distance = Float.MAX_VALUE

    private var isSetup = false
    private var textSize: Float = 0f
    private var northString: String = ""
    private var southString: String = ""
    private var eastString: String = ""
    private var westString: String = ""

    private val lineLayer = ARLineLayer()

    private val resolution = 6

    init {
        val regularLines = mutableListOf<List<ARPoint>>()

        // Horizontal lines
        for (elevation in -90..90 step spacing) {
            // Skip horizon
            if (elevation == 0) {
                continue
            }

            val line = mutableListOf<ARPoint>()
            for (azimuth in 0..360 step resolution) {
                line.add(
                    SphericalARPoint(
                        azimuth.toFloat(),
                        elevation.toFloat(),
                        isTrueNorth = useTrueNorth
                    )
                )
            }
            regularLines.add(line)
        }

        // Vertical lines
        for (azimuth in 0 until 360 step spacing) {

            // Skip north
            if (azimuth == 0) {
                continue
            }

            val line = mutableListOf<ARPoint>()
            for (elevation in -90..90 step resolution) {
                line.add(
                    SphericalARPoint(
                        azimuth.toFloat(),
                        elevation.toFloat(),
                        isTrueNorth = useTrueNorth
                    )
                )
            }
            regularLines.add(line)
        }

        // North line
        val northLine = mutableListOf<ARPoint>()
        for (elevation in -90..90 step resolution) {
            northLine.add(SphericalARPoint(0f, elevation.toFloat(), isTrueNorth = useTrueNorth))
        }

        // Horizon line
        val horizonLine = mutableListOf<ARPoint>()
        for (azimuth in 0..360 step resolution) {
            horizonLine.add(SphericalARPoint(azimuth.toFloat(), 0f, isTrueNorth = useTrueNorth))
        }

        lineLayer.setLines(
            regularLines.map { ARLine(it, color, thicknessDp) } +
                    ARLine(northLine, northColor, thicknessDp) +
                    ARLine(horizonLine, horizonColor, thicknessDp)
        )
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {

        if (!isSetup) {
            textSize = drawer.sp(16f)
            northString = view.context.getString(R.string.direction_north)
            southString = view.context.getString(R.string.direction_south)
            eastString = view.context.getString(R.string.direction_east)
            westString = view.context.getString(R.string.direction_west)
            isSetup = true
        }

        lineLayer.draw(drawer, view)

        // Draw cardinal direction labels
        val offset = 2f
        val north = view.toPixel(
            AugmentedRealityCoordinate.fromSpherical(
                0f,
                offset,
                distance,
                useTrueNorth
            )
        )
        val south = view.toPixel(
            AugmentedRealityCoordinate.fromSpherical(
                180f,
                offset,
                distance,
                useTrueNorth
            )
        )
        val east = view.toPixel(
            AugmentedRealityCoordinate.fromSpherical(
                90f,
                offset,
                distance,
                useTrueNorth
            )
        )
        val west = view.toPixel(
            AugmentedRealityCoordinate.fromSpherical(
                -90f,
                offset,
                distance,
                useTrueNorth
            )
        )

        drawLabel(drawer, view, northString, north)
        drawLabel(drawer, view, southString, south)
        drawLabel(drawer, view, eastString, east)
        drawLabel(drawer, view, westString, west)
    }

    private fun drawLabel(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        text: String,
        position: PixelCoordinate
    ) {
        drawer.textSize(drawer.sp(16f))
        drawer.fill(labelColor)
        drawer.noStroke()
        drawer.push()
        drawer.rotate(view.sideInclination, position.x, position.y)
        drawer.text(text, position.x, position.y)
        drawer.pop()
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        return false
    }
}