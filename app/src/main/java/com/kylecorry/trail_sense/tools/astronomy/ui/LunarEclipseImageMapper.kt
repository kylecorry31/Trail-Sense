package com.kylecorry.trail_sense.tools.astronomy.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.science.astronomy.eclipse.LunarEclipseShadow
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.astronomy.domain.MoonTilt

class LunarEclipseImageMapper(private val context: Context) {

    private val moonDrawable by lazy {
        Resources.drawable(context, R.drawable.ic_moon)
    }

    fun getEclipseImage(
        moon: CelestialObservation,
        shadow: LunarEclipseShadow,
        width: Int,
        height: Int,
        tilt: MoonTilt? = null
    ): Bitmap {
        val output = createBitmap(width, height)
        val canvas = Canvas(output)
        val drawer = CanvasDrawer(context, canvas)
        drawer.push()
        tilt?.let {
            drawer.rotate(it.parallacticAngle, width / 2f, height / 2f)
        }
        moonDrawable?.let {
            it.setBounds(0, 0, width, height)
            it.draw(canvas)
        }
        drawer.pop()

        val shadowOffset = Trigonometry.getAngularOffset(
            moon.azimuth.value,
            moon.altitude,
            shadow.umbra.azimuth.value,
            shadow.umbra.altitude
        )
        val moonAngularDiameter = moon.angularDiameter ?: 0.5f
        val umbraAngularDiameter = shadow.umbra.angularDiameter ?: 0.5f
        val penumbraAngularDiameter = shadow.penumbra.angularDiameter ?: umbraAngularDiameter
        val scale = minOf(width, height) / moonAngularDiameter
        val centerX = width / 2f
        val centerY = height / 2f
        val shadowCenterX = centerX + shadowOffset.x * scale
        val shadowCenterY = centerY - shadowOffset.y * scale
        val umbraRadius = umbraAngularDiameter / 2f * scale
        val penumbraRadius = penumbraAngularDiameter / 2f * scale

        val moonClip = Path().apply {
            addCircle(
                centerX,
                centerY,
                minOf(width, height) / 2f + CLIP_BUFFER_PIXELS,
                Path.Direction.CW
            )
        }
        drawer.noStroke()
        drawer.push()
        drawer.clip(moonClip)
        drawer.fill(SHADOW_COLOR)
        drawer.opacity(PENUMBRA_ALPHA)
        drawer.circle(shadowCenterX, shadowCenterY, penumbraRadius * 2f)
        drawer.opacity(255)
        drawer.fill(SHADOW_COLOR)
        drawer.circle(shadowCenterX, shadowCenterY, umbraRadius * 2f)
        drawer.pop()

        return output
    }

    companion object {
        private const val SHADOW_COLOR = 0x80FF6D00.toInt()
        private const val CLIP_BUFFER_PIXELS = 0.5f
        private const val PENUMBRA_ALPHA = 32
    }
}
