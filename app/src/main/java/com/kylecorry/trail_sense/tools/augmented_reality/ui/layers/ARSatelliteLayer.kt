package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.SphericalARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARMarker
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.ui.CanvasBitmap
import com.kylecorry.trail_sense.tools.augmented_reality.ui.CanvasCircle
import com.kylecorry.trail_sense.tools.navigation.ui.DrawerBitmapLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ARSatelliteLayer(
    private val gps: ISatelliteGPS,
    private val onSatelliteFocus: (satellite: Satellite) -> Boolean,
) : ARLayer {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val runner = CoroutineQueueRunner()

    private val satelliteLayer = ARMarkerLayer()

    private var bitmapLoader: DrawerBitmapLoader? = null

    private val hooks = Hooks()

    override suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView) {

        hooks.effect(
            "satellites",
            gps.satelliteDetails,
        ) {
            updatePositions(drawer)
        }

        satelliteLayer.update(drawer, view)
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        satelliteLayer.draw(drawer, view)
    }

    override fun invalidate() {
        satelliteLayer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return satelliteLayer.onClick(drawer, view, pixel)
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        return satelliteLayer.onFocus(drawer, view)
    }

    private fun updatePositions(
        drawer: ICanvasDrawer
    ) {
        scope.launch {
            runner.enqueue {
                if (bitmapLoader == null) {
                    bitmapLoader = DrawerBitmapLoader(drawer)
                }

                val satelliteImageSize = drawer.dp(24f).toInt()
                val satelliteBitmap = bitmapLoader?.load(R.drawable.satellite, satelliteImageSize)


                val satelliteMarkers = gps.satelliteDetails?.map {
                    ARMarker(
                        SphericalARPoint(
                            it.azimuth,
                            it.elevation,
                            isTrueNorth = true,
                            angularDiameter = 1f
                        ),
                        canvasObject = satelliteBitmap?.let {
                            CanvasBitmap(
                                satelliteBitmap,
                                tint = Color.WHITE
                            )
                        } ?: CanvasCircle(Color.WHITE),
                        onFocusedFn = {
                            onSatelliteFocus(it)
                        }
                    )
                }

                satelliteLayer.setMarkers(satelliteMarkers ?: emptyList())
            }
        }
    }

}