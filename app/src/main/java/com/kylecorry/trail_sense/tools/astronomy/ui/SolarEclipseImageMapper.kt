package com.kylecorry.trail_sense.tools.astronomy.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils

class SolarEclipseImageMapper(private val context: Context) {

    fun getEclipseImage(
        sun: CelestialObservation,
        moon: CelestialObservation,
        isTotal: Boolean,
        width: Int,
        height: Int
    ): Bitmap {
        val output = createBitmap(width, height)
        val canvas = Canvas(output)
        val drawer = CanvasDrawer(context, canvas)
        val moonOffset = AugmentedRealityUtils.getAngularOffset(
            sun.azimuth.value,
            sun.altitude,
            moon.azimuth.value,
            moon.altitude
        )
        val sunAngularDiameter = sun.angularDiameter ?: 0.5f
        val moonAngularDiameter = moon.angularDiameter ?: 0.5f

        val sunAngularRadius = sunAngularDiameter / 2f
        val moonAngularRadius = moonAngularDiameter / 2f
        val scale = minOf(width, height) / sunAngularDiameter
        val sunRadius = sunAngularRadius * scale
        val moonRadius = moonAngularRadius * scale
        val centerX = width / 2f
        val centerY = height / 2f

        val moonCenterX = centerX + moonOffset.x * scale
        val moonCenterY = centerY - moonOffset.y * scale
        drawer.noStroke()
        drawer.fill(Resources.color(context, R.color.sun))
        drawer.circle(centerX, centerY, sunRadius * 2f)
        drawer.fill(MOON_COLOR)
        val sunClip = Path().apply {
            addCircle(centerX, centerY, sunRadius + CLIP_BUFFER_PIXELS, Path.Direction.CW)
        }
        drawer.push()
        drawer.clip(sunClip)
        if (isTotal) {
            // Render the total eclipse as a ring of the sun with the moon centered
            drawer.circle(centerX, centerY, sunRadius * 2f * TOTAL_ECLIPSE_MOON_PERCENT_DIAMETER)
        } else {
            drawer.circle(moonCenterX, moonCenterY, moonRadius * 2f)
        }
        drawer.pop()

        return output
    }

    companion object {
        private const val MOON_COLOR = Color.BLACK
        private const val TOTAL_ECLIPSE_MOON_PERCENT_DIAMETER = 0.95f
        private const val CLIP_BUFFER_PIXELS = 0.5f
    }
}


