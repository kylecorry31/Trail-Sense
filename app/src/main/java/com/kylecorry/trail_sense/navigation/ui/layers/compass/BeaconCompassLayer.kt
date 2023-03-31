package com.kylecorry.trail_sense.navigation.ui.layers.compass

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.ui.IMappableReferencePoint
import com.kylecorry.trail_sense.navigation.ui.MappableReferencePoint
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BeaconCompassLayer(
    private val size: Float = 24f
) : CompassMarkerLayer() {

    private val _beacons = mutableListOf<Beacon>()
    private var _highlighted: Beacon? = null

    @ColorInt
    private var backgroundColor = Color.TRANSPARENT

    private val lock = Any()

    private val runner = ControlledRunner<Unit>()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var _compass: ICompassView? = null

    fun setBeacons(beacons: List<Beacon>) {
        synchronized(lock) {
            _beacons.clear()
            _beacons.addAll(beacons)
        }
        _compass?.let { updateMarkers(it) }
    }

    override fun draw(drawer: ICanvasDrawer, compass: ICompassView) {
        var shouldUpdate = false

        if (_compass == null) {
            _compass = compass
            shouldUpdate = true
        }

        if (shouldUpdate) {
            updateMarkers(compass)
        }

        super.draw(drawer, compass)
    }

    fun setOutlineColor(@ColorInt color: Int) {
        backgroundColor = color
        _compass?.let { updateMarkers(it) }
    }

    fun highlight(beacon: Beacon?) {
        _highlighted = beacon
        _compass?.let { updateMarkers(it) }
    }

    private fun updateMarkers(compass: ICompassView) {
        scope.launch {
            runner.cancelPreviousThenRun {
                synchronized(lock) {
                    val markers = convertToMarkers(_beacons, compass)
                    clearMarkers()
                    for (marker in markers) {
                        addMarker(marker.first, marker.second)
                    }
                }
            }
        }
    }

    // TODO: Load the icon here
    private fun convertToMarkers(
        beacons: List<Beacon>,
        compass: ICompassView
    ): List<Pair<IMappableReferencePoint, Int?>> {
//        val loader = _loader ?: return emptyList()
        val markers = mutableListOf<Pair<IMappableReferencePoint, Int?>>()
        beacons.forEach {

            // Reduce the opacity if the beacon is not highlighted
            val opacity = if (_highlighted == null || _highlighted?.id == it.id) {
                1f
            } else {
                0.5f
            }

            val bearing = if (compass.useTrueNorth) {
                compass.compassCenter.bearingTo(it.coordinate)
            } else {
                DeclinationUtils.fromTrueNorthBearing(
                    compass.compassCenter.bearingTo(it.coordinate),
                    compass.declination
                )
            }

            // Background
            markers.add(
                MappableReferencePoint(
                    it.id,
                    R.drawable.ic_arrow_target,
                    bearing,
                    it.color,
                    opacity = opacity
                ) to null
            )

            it.icon?.let { icon ->
                markers.add(
                    MappableReferencePoint(
                        it.id,
                        icon.icon,
                        bearing,
                        Colors.mostContrastingColor(Color.WHITE, Color.BLACK, it.color),
                        opacity = opacity
                    ) to (size * 0.35f).toInt()
                )
            }
            // Create the icon for the marker
//            if (it.icon != null) {
//                val image = loader.load(it.icon.icon, size)
//                val color =
//                    Colors.mostContrastingColor(Color.WHITE, Color.BLACK, it.color)
//                markers.add(
//                    BitmapMapMarker(
//                        it.coordinate,
//                        image,
//                        size = this.size * 0.75f,
//                        tint = color
//                    ) {
//                        onBeaconClick(it)
//                    })
//            }
        }
        return markers
    }

}