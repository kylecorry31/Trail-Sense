package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.kylecorry.sol.science.geography.projections.AzimuthalEquidistantProjection
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.IMappableBearing
import com.kylecorry.trail_sense.shared.map_layers.IMappableReferencePoint
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView

class RadarCompassMapView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs), ICompassView {

    val map: MapView
    private val compass: RadarCompassView

    init {
        inflate(context, R.layout.view_radar_compass_map, this)
        map = findViewById(R.id.radar_compass_map)
        compass = findViewById(R.id.radar_compass_dial)
        compass.bindMapView(map)
    }

    override var compassCenter: Coordinate
        get() = compass.compassCenter
        set(value) {
            compass.compassCenter = value
            map.mapCenter = value
            map.userLocation = value
            map.projection = AzimuthalEquidistantProjection(value)
        }

    override var useTrueNorth: Boolean
        get() = compass.useTrueNorth
        set(value) {
            compass.useTrueNorth = value
        }

    override var declination: Float
        get() = compass.declination
        set(value) {
            compass.declination = value
        }

    override var azimuth: Float
        get() = compass.azimuth
        set(value) {
            compass.azimuth = value
        }

    var trueNorthAzimuth: Bearing
        get() = map.userAzimuth
        set(value) {
            map.userAzimuth = value
            map.mapAzimuth = value.value
        }

    var locationAccuracy: Distance?
        get() = map.userLocationAccuracy
        set(value) {
            map.userLocationAccuracy = value
        }

    var shouldDrawDial: Boolean
        get() = compass.shouldDrawDial
        set(value) {
            compass.shouldDrawDial = value
        }

    var shouldDrawAzimuthIndicator: Boolean
        get() = compass.shouldDrawAzimuthIndicator
        set(value) {
            compass.shouldDrawAzimuthIndicator = value
        }

    fun setOnSingleTapListener(listener: (() -> Unit)?) {
        map.setOnSingleTapListener(listener?.let { { _ -> it() } })
    }

    fun setOnLongPressListener(listener: (() -> Unit)?) {
        map.setOnLongPressListener(listener?.let { { _ -> it() } })
    }

    override fun addCompassLayer(layer: ICompassLayer) {
        compass.addCompassLayer(layer)
    }

    override fun removeCompassLayer(layer: ICompassLayer) {
        compass.removeCompassLayer(layer)
    }

    override fun setCompassLayers(layers: List<ICompassLayer>) {
        compass.setCompassLayers(layers)
    }

    override fun draw(reference: IMappableReferencePoint, size: Int?) {
        compass.draw(reference, size)
    }

    override fun draw(bearing: IMappableBearing, stopAt: Coordinate?) {
        compass.draw(bearing, stopAt)
    }

    override fun redraw() {
        compass.redraw()
    }
}
