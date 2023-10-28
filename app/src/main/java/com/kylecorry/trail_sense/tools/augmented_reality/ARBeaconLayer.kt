package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.positive
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.ui.DrawerBitmapLoader
import com.kylecorry.trail_sense.shared.canvas.PixelCircle

class ARBeaconLayer(
    private val labelFormatter: (beacon: Beacon, distance: Distance) -> String? = { beacon, _ -> beacon.name }
) : ARLayer {

    private val beacons = mutableListOf<Beacon>()
    private val lock = Any()
    private var _loader: DrawerBitmapLoader? = null
    private var loadedImageSize = 24

    fun setBeacons(beacons: List<Beacon>) {
        synchronized(lock) {
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
            val distance = view.location.distanceTo(it.coordinate)
            if (distance > view.viewDistance.meters().distance) {
                return@mapNotNull null
            }
            it to distance
        }.sortedBy { it.second }

        var hasShownText = false

        // Draw the beacons
        visible.forEach {
            val pixel = view.toPixel(it.first.coordinate, it.first.elevation)
            // TODO: Pass in angular size (or maybe just size, and scale that)
            val originalSize = (360f / it.second.positive(1f)).coerceIn(1f, 5f)
            val diameter = view.sizeToPixel(originalSize)
            // Draw a circle
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

            // If it is centered, also draw the label
            // TODO: Figure out how the reticle should be handled - is the label even this layer's responsibility?
            val center = PixelCoordinate(view.width / 2f, view.height / 2f)
            val centerCircle = PixelCircle(center, view.reticleSize / 2f)
            val circle = PixelCircle(pixel, diameter / 2f)

            if (!hasShownText && circle.intersects(centerCircle)) {
                val text = labelFormatter(it.first, Distance.meters(it.second))
                if (!text.isNullOrBlank()) {
                    drawer.push()
                    drawer.rotate(view.sideInclination, pixel.x, pixel.y)
                    drawer.fill(Color.WHITE)
                    drawer.textSize(drawer.sp(16f))
                    drawer.textMode(TextMode.Corner)
                    drawer.textAlign(TextAlign.Center)
                    val textH = drawer.textHeight(text)

                    // Handle newlines (TODO: Do this in andromeda)
                    val lines = text.split("\n")
                    val start = pixel.y + textH + diameter / 2f + drawer.dp(8f)
                    val lineSpacing = drawer.sp(4f)
                    lines.forEachIndexed { index, line ->
                        drawer.text(line, pixel.x, start + index * (textH + lineSpacing))
                    }
                    hasShownText = true
                    drawer.pop()
                }
            }

        }


    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer, view: AugmentedRealityView, pixel: PixelCoordinate
    ): Boolean {
        return false
    }

}