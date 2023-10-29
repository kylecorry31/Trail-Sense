package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.ui.DrawerBitmapLoader
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.text
import kotlin.math.hypot

// TODO: Figure out what to pass for the visible distance: d = 1.2246 * sqrt(h) where d is miles and h is feet (or move it to the consumer)
class ARBeaconLayer(
    var maxVisibleDistance: Distance = Distance.kilometers(1f),
    private val beaconSize: Distance = Distance.meters(4f),
    private val labelFormatter: (beacon: Beacon, distance: Distance) -> String? = { beacon, _ -> beacon.name }
) : ARLayer {

    private val beacons = mutableListOf<Beacon>()
    private val lock = Any()
    private var _loader: DrawerBitmapLoader? = null
    private var loadedImageSize = 24

    private var focusedBeacon: Beacon? = null

    fun setBeacons(beacons: List<Beacon>) {
        synchronized(lock) {
            // TODO: Convert to markers
            this.beacons.clear()
            this.beacons.addAll(beacons)
        }
    }

    fun addBeacon(beacon: Beacon) {
        synchronized(lock) {
            beacons.add(beacon)
        }
    }

    fun removeBeacon(beacon: Beacon) {
        synchronized(lock) {
            beacons.remove(beacon)
        }
    }

    fun clearBeacons() {
        synchronized(lock) {
            beacons.clear()
        }
    }

    protected fun finalize() {
        _loader?.clear()
        _loader = null
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        if (_loader == null) {
            _loader = DrawerBitmapLoader(drawer)
            loadedImageSize = drawer.dp(24f).toInt()
        }

        val minBeaconPixels = drawer.dp(8f)
        val maxBeaconPixels = drawer.dp(48f)

        val loader = _loader ?: return

        val beacons = synchronized(lock) {
            beacons.toList()
        }

        // TODO: Is this the responsibility of the layer or consumer?
        // Filter to the beacons which are visible and within the viewDistance
        val visible = beacons.mapNotNull {
            if (!it.visible) {
                return@mapNotNull null
            }
            val distance = hypot(
                view.location.distanceTo(it.coordinate),
                (it.elevation ?: view.altitude) - view.altitude
            )
            if (distance > maxVisibleDistance.meters().distance) {
                return@mapNotNull null
            }
            it to distance
        }.sortedByDescending { it.second }

        focusedBeacon = null

        val center = PixelCoordinate(view.width / 2f, view.height / 2f)
        val centerCircle = PixelCircle(center, view.reticleDiameter / 2f)

        // Draw the beacons
        visible.forEach {
            val pixel = view.toPixel(it.first.coordinate, it.first.elevation)
            val diameter = view.sizeToPixel(beaconSize, Distance.meters(it.second))
                .coerceIn(minBeaconPixels, maxBeaconPixels)

            // Draw a circle for the beacon
            drawer.strokeWeight(drawer.dp(0.5f))
            drawer.stroke(Color.WHITE)
            drawer.fill(it.first.color)
            drawer.circle(pixel.x, pixel.y, diameter)

            // Draw the icon
            if (it.first.icon != null) {
                val image = loader.load(it.first.icon!!.icon, loadedImageSize)
                val color =
                    Colors.mostContrastingColor(Color.WHITE, Color.BLACK, it.first.color)
                drawer.push()
                drawer.rotate(view.sideInclination, pixel.x, pixel.y)
                drawer.tint(color)
                drawer.imageMode(ImageMode.Center)
                drawer.image(image, pixel.x, pixel.y, diameter * 0.75f, diameter * 0.75f)
                drawer.noTint()
                drawer.pop()
            }

            // Update the focused beacon
            val circle = PixelCircle(pixel, diameter / 2f)
            if (circle.intersects(centerCircle)) {
                focusedBeacon = it.first
            }
        }
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer, view: AugmentedRealityView, pixel: PixelCoordinate
    ): Boolean {
        // TODO: Expose this to the consumer
        return false
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        // TODO: Move this to the consumer
        val focused = focusedBeacon ?: return false
        val distance = hypot(
            view.location.distanceTo(focused.coordinate),
            (focused.elevation ?: view.altitude) - view.altitude
        )
        val textToRender = labelFormatter(focused, Distance.meters(distance))
        if (!textToRender.isNullOrBlank()) {
            view.focusText = textToRender
            return true
        }
        return false
    }

}