package com.kylecorry.trail_sense.tools.signal_finder.map_layers

import android.graphics.Bitmap
import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.text.StringLoader
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.markers.BitmapMapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.CircleMapMarker
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel

class CellTowerMapLayer(
    private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask(),
    private val onClick: (tower: ApproximateCoordinate) -> Boolean = { false }
) :
    IAsyncLayer, BaseLayer() {

    private var bitmap: Bitmap? = null

    private val minZoomLevel = 10
    private var updateListener: (() -> Unit)? = null

    init {
        taskRunner.addTask { bounds, metersPerPixel ->
            val zoomLevel = TileMath.distancePerPixelToZoom(
                metersPerPixel.toDouble(),
                (bounds.north + bounds.south) / 2
            )

            if (zoomLevel < minZoomLevel) {
                return@addTask
            }

            val towers = CellTowerModel.getTowers(bounds)
            clearMarkers()
            towers.forEach {
                val sizePixels = 2 * it.accuracy.meters().value / metersPerPixel
                addMarker(
                    CircleMapMarker(
                        it.coordinate,
                        Color.WHITE,
                        null,
                        25,
                        sizePixels,
                        isSizeInDp = false,
                        useScale = false
                    )
                )

                bitmap?.let { bitmap ->
                    addMarker(
                        BitmapMapMarker(
                            it.coordinate,
                            bitmap,
                            tint = Color.WHITE
                        ) {
                            onClick(it)
                        }
                    )
                }
            }
            updateListener?.invoke()

        }
    }

    fun setPreferences(prefs: CellTowerMapLayerPreferences) {
        setPercentOpacity(prefs.opacity.get() / 100f)
        invalidate()
    }

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        if (SafeMode.isEnabled() || map.metersPerPixel > 75f) {
            return
        }

        if (bitmap == null) {
            val size = drawer.dp(12f).toInt()
            bitmap = drawer.loadImage(R.drawable.cell_tower, size, size)
        }

        taskRunner.scheduleUpdate(map.mapBounds, map.metersPerPixel)
        super.draw(drawer, map)
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }

    protected fun finalize() {
        bitmap?.recycle()
        bitmap = null
    }

    companion object {
        fun navigate(tower: ApproximateCoordinate){
            val navigator = getAppService<Navigator>()
            val strings = getAppService<StringLoader>()
            navigator.navigateTo(
                tower.coordinate,
                strings.getString(R.string.cell_tower),
                BeaconOwner.Maps
            )
        }
    }
}