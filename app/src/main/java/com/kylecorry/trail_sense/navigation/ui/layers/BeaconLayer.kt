package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.ui.markers.CircleMapMarker

class BeaconLayer(private val onBeaconClick: (beacon: Beacon) -> Boolean = { false }) :
    BaseLayer() {

    private val _beacons = mutableListOf<Beacon>()
    private var _highlighted: Beacon? = null

    @ColorInt
    private var backgroundColor = Color.TRANSPARENT

    fun setBeacons(beacons: List<Beacon>) {
        _beacons.clear()
        _beacons.addAll(beacons)
        updateMarkers()
    }

    fun setOutlineColor(@ColorInt color: Int) {
        backgroundColor = color
        updateMarkers()
    }

    fun highlight(beacon: Beacon?) {
        _highlighted = beacon
        updateMarkers()
    }

    private fun updateMarkers() {
        clearMarkers()
        _beacons.forEach {
            val opacity = if (_highlighted == null || _highlighted?.id == it.id) {
                255
            } else {
                127
            }
            addMarker(CircleMapMarker(it.coordinate, it.color, backgroundColor, opacity) {
                onBeaconClick(it)
            })
        }
    }
}