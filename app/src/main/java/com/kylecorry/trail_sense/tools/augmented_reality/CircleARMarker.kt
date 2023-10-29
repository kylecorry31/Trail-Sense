package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import kotlin.math.hypot

class CircleARMarker private constructor(
    private val position: AugmentedRealityView.HorizonCoordinate?,
    private val angularDiameter: Float?,
    private val location: Coordinate?,
    private val elevation: Float?,
    private val actualDiameter: Float?,
    @ColorInt
    private val color: Int,
    @ColorInt
    private val strokeColor: Int? = null,
    private val opacity: Int = 255,
    private val strokeWeight: Float = 0.5f,
    private val onFocusedFn: (() -> Boolean) = { false },
    private val onClickFn: () -> Boolean = { false }
) {
    fun draw(drawer: ICanvasDrawer, anchor: PixelCircle) {
        val size = anchor.radius * 2f
        drawer.noTint()
        if (strokeColor != null && strokeColor != Color.TRANSPARENT) {
            drawer.stroke(strokeColor)
            drawer.strokeWeight(drawer.dp(strokeWeight))
        } else {
            drawer.noStroke()
        }
        if (color != Color.TRANSPARENT) {
            drawer.fill(color)
            drawer.opacity(opacity)
            drawer.circle(anchor.center.x, anchor.center.y, size)
        }
    }

    fun getAngularDiameter(view: AugmentedRealityView): Float {
        if (actualDiameter != null && location != null) {
            val distance = hypot(
                view.location.distanceTo(location),
                (elevation ?: view.altitude) - view.altitude
            )
            return AugmentedRealityUtils.getAngularSize(actualDiameter, distance)
        }
        return angularDiameter ?: 1f
    }

    fun getHorizonCoordinate(view: AugmentedRealityView): AugmentedRealityView.HorizonCoordinate {
        if (location != null) {
            return AugmentedRealityUtils.getHorizonCoordinate(
                view.location,
                view.altitude,
                location,
                elevation
            )
        }

        return position ?: AugmentedRealityView.HorizonCoordinate(0f, 0f)
    }


    fun onFocused(): Boolean {
        return onFocusedFn()
    }

    fun onClick(): Boolean {
        return onClickFn()
    }

    companion object {
        fun horizon(
            position: AugmentedRealityView.HorizonCoordinate?,
            angularDiameter: Float = 12f,
            @ColorInt color: Int,
            @ColorInt strokeColor: Int? = null,
            opacity: Int = 255,
            strokeWeight: Float = 0.5f,
            onFocusedFn: (() -> Boolean) = { false },
            onClickFn: () -> Boolean = { false }
        ): CircleARMarker {
            return CircleARMarker(
                position,
                angularDiameter,
                null,
                null,
                null,
                color,
                strokeColor,
                opacity,
                strokeWeight,
                onFocusedFn,
                onClickFn
            )
        }

        fun geographic(
            location: Coordinate,
            elevation: Float,
            actualDiameter: Float,
            @ColorInt color: Int,
            @ColorInt strokeColor: Int? = null,
            opacity: Int = 255,
            strokeWeight: Float = 0.5f,
            onFocusedFn: (() -> Boolean) = { false },
            onClickFn: () -> Boolean = { false }
        ): CircleARMarker {
            return CircleARMarker(
                null,
                null,
                location,
                elevation,
                actualDiameter,
                color,
                strokeColor,
                opacity,
                strokeWeight,
                onFocusedFn,
                onClickFn
            )
        }
    }
}