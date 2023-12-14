package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.navigation.ui.DrawerBitmapLoader
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.augmented_reality.position.SphericalARPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ARAstronomyLayer(
    private val onSunFocus: (time: ZonedDateTime) -> Boolean,
    private val onMoonFocus: (time: ZonedDateTime) -> Boolean
) : ARLayer {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val runner = CoroutineQueueRunner()

    private val sunLayer = ARMarkerLayer()
    private val moonLayer = ARMarkerLayer()

    private val astro = AstronomyService()

    private var bitmapLoader: DrawerBitmapLoader? = null

    private var lastUpdateTime = 0L
    private var lastUpdateLocation = Coordinate.zero

    private val updateFrequency = Duration.ofMinutes(1).toMillis()
    private val updateDistance = 1000f

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        val location = view.location
        val time = System.currentTimeMillis()

        if (location.distanceTo(lastUpdateLocation) > updateDistance || time - lastUpdateTime > updateFrequency) {
            lastUpdateTime = time
            lastUpdateLocation = location
            updatePositions(drawer, location, ZonedDateTime.now())
        }

        moonLayer.draw(drawer, view)
        sunLayer.draw(drawer, view)
    }

    override fun invalidate() {
        moonLayer.invalidate()
        sunLayer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return sunLayer.onClick(drawer, view, pixel) || moonLayer.onClick(drawer, view, pixel)
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        return sunLayer.onFocus(drawer, view) || moonLayer.onFocus(drawer, view)
    }

    private fun updatePositions(drawer: ICanvasDrawer, location: Coordinate, time: ZonedDateTime) {
        scope.launch {
            runner.enqueue {
                if (bitmapLoader == null) {
                    bitmapLoader = DrawerBitmapLoader(drawer)
                }

                val moonBeforePathObject = CanvasCircle(
                    Color.WHITE,
                    opacity = 20
                )

                val moonAfterPathObject = CanvasCircle(
                    Color.WHITE,
                    opacity = 127
                )

                val sunBeforePathObject = CanvasCircle(
                    AppColor.Yellow.color,
                    opacity = 20
                )

                val sunAfterPathObject = CanvasCircle(
                    AppColor.Yellow.color,
                    opacity = 127
                )

                val moonPositions = Time.getReadings(
                    LocalDate.now(),
                    ZoneId.systemDefault(),
                    Duration.ofMinutes(15)
                ) {
                    val obj = if (it.isBefore(ZonedDateTime.now())) {
                        moonBeforePathObject
                    } else {
                        moonAfterPathObject
                    }

                    ARMarker(
                        SphericalARPoint(
                            astro.getMoonAzimuth(location, it).value,
                            astro.getMoonAltitude(location, it),
                            isTrueNorth = true,
                            angularDiameter = 1f
                        ),
                        canvasObject = obj,
                        onFocusedFn = {
                            onMoonFocus(it)
                        }
                    )
                }.map { it.value }

                val sunPositions = Time.getReadings(
                    LocalDate.now(), ZoneId.systemDefault(), Duration.ofMinutes(15)
                ) {
                    val obj = if (it.isBefore(ZonedDateTime.now())) {
                        sunBeforePathObject
                    } else {
                        sunAfterPathObject
                    }

                    ARMarker(
                        SphericalARPoint(
                            astro.getSunAzimuth(location, it).value,
                            astro.getSunAltitude(location, it),
                            isTrueNorth = true,
                            angularDiameter = 1f
                        ),
                        canvasObject = obj,
                        onFocusedFn = {
                            onSunFocus(it)
                        }
                    )
                }.map { it.value }

                val moonAltitude = astro.getMoonAltitude(location)
                val moonAzimuth = astro.getMoonAzimuth(location).value

                val sunAltitude = astro.getSunAltitude(location)
                val sunAzimuth = astro.getSunAzimuth(location).value

                val phase = astro.getMoonPhase(LocalDate.now())
                val moonIconId = MoonPhaseImageMapper().getPhaseImage(phase.phase)
                val moonImageSize = drawer.dp(24f).toInt()
                val moonBitmap = bitmapLoader?.load(moonIconId, moonImageSize)

                val moon = ARMarker(
                    SphericalARPoint(
                        moonAzimuth,
                        moonAltitude,
                        isTrueNorth = true,
                        angularDiameter = 2f
                    ),
                    canvasObject = moonBitmap?.let { CanvasBitmap(moonBitmap) }
                        ?: CanvasCircle(Color.WHITE),
                    onFocusedFn = {
                        onMoonFocus(ZonedDateTime.now())
                    }
                )

                val sun = ARMarker(
                    SphericalARPoint(
                        sunAzimuth,
                        sunAltitude,
                        isTrueNorth = true,
                        angularDiameter = 2f
                    ),
                    canvasObject = CanvasCircle(AppColor.Yellow.color),
                    onFocusedFn = {
                        onSunFocus(ZonedDateTime.now())
                    }
                )

                sunLayer.setMarkers(sunPositions + sun)
                moonLayer.setMarkers(moonPositions + moon)
            }
        }
    }
}