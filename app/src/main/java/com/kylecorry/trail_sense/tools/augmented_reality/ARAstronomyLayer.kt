package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
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
import java.time.ZonedDateTime

class ARAstronomyLayer(
    private val drawLines: Boolean,
    private val drawBelowHorizon: Boolean,
    private val onSunFocus: (time: ZonedDateTime) -> Boolean,
    private val onMoonFocus: (time: ZonedDateTime) -> Boolean
) : ARLayer {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val runner = CoroutineQueueRunner()

    private val lineAlpha = 30
    private val lineThickness = 1f

    private val sunLineLayer = ARLineLayer(
        AppColor.Yellow.color.withAlpha(lineAlpha),
        thickness = lineThickness,
        thicknessType = ARLineLayer.ThicknessType.Angle,
        curved = true
    )
    private val sunLayer = ARMarkerLayer()
    private val currentSunLayer = ARMarkerLayer()

    private val moonLineLayer = ARLineLayer(
        Color.WHITE.withAlpha(lineAlpha),
        thickness = lineThickness,
        thicknessType = ARLineLayer.ThicknessType.Angle,
        curved = true
    )
    private val moonLayer = ARMarkerLayer()
    private val currentMoonLayer = ARMarkerLayer()

    private val astro = AstronomyService()

    private var bitmapLoader: DrawerBitmapLoader? = null

    private var lastUpdateTime = 0L
    private var lastUpdateLocation = Coordinate.zero

    private val updateFrequency = Duration.ofMinutes(1).toMillis()
    private val updateDistance = 1000f

    // TODO: Eventually use a clip path to clip the lines to the horizon
    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        val location = view.location
        val time = System.currentTimeMillis()

        if (location.distanceTo(lastUpdateLocation) > updateDistance || time - lastUpdateTime > updateFrequency) {
            lastUpdateTime = time
            lastUpdateLocation = location
            updatePositions(drawer, location, ZonedDateTime.now())
        }

        if (drawLines) {
            moonLineLayer.draw(drawer, view)
            sunLineLayer.draw(drawer, view)
        }
        moonLayer.draw(drawer, view)
        sunLayer.draw(drawer, view)
        currentMoonLayer.draw(drawer, view)
        currentSunLayer.draw(drawer, view)
    }

    override fun invalidate() {
        moonLineLayer.invalidate()
        sunLineLayer.invalidate()
        moonLayer.invalidate()
        sunLayer.invalidate()
        currentMoonLayer.invalidate()
        currentSunLayer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return currentSunLayer.onClick(drawer, view, pixel) ||
                currentMoonLayer.onClick(drawer, view, pixel) ||
                sunLayer.onClick(drawer, view, pixel) ||
                moonLayer.onClick(drawer, view, pixel)
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        return currentSunLayer.onFocus(drawer, view) ||
                currentMoonLayer.onFocus(drawer, view) ||
                sunLayer.onFocus(drawer, view) ||
                moonLayer.onFocus(drawer, view)
    }

    private fun updatePositions(
        drawer: ICanvasDrawer,
        location: Coordinate,
        time: ZonedDateTime
    ) {
        scope.launch {
            runner.enqueue {
                if (bitmapLoader == null) {
                    bitmapLoader = DrawerBitmapLoader(drawer)
                }

                val granularity = Duration.ofMinutes(10)

                val moonBeforePathObject = CanvasCircle(
                    Color.WHITE,
                    opacity = 60
                )

                val moonAfterPathObject = CanvasCircle(
                    Color.WHITE,
                    opacity = 200
                )

                val sunBeforePathObject = CanvasCircle(
                    AppColor.Yellow.color,
                    opacity = 60
                )

                val sunAfterPathObject = CanvasCircle(
                    AppColor.Yellow.color,
                    opacity = 200
                )

                // TODO: Should this be the day's value or the values when it is above the horizon?
                val moonPositions = Time.getReadings(
                    time.toLocalDate(),
                    time.zone,
                    granularity
                ) {
                    val obj = if (it.isBefore(time)) {
                        moonBeforePathObject
                    } else {
                        moonAfterPathObject
                    }

                    ARMarker(
                        SphericalARPoint(
                            astro.getMoonAzimuth(location, it).value,
                            astro.getMoonAltitude(location, it),
                            isTrueNorth = true,
                            angularDiameter = 0.5f
                        ),
                        canvasObject = obj,
                        onFocusedFn = {
                            onMoonFocus(it)
                        }
                    )
                }.map { it.value }

                val sunPositions = Time.getReadings(
                    time.toLocalDate(),
                    time.zone,
                    granularity
                ) {

                    val obj = if (it.isBefore(time)) {
                        sunBeforePathObject
                    } else {
                        sunAfterPathObject
                    }

                    ARMarker(
                        SphericalARPoint(
                            astro.getSunAzimuth(location, it).value,
                            astro.getSunAltitude(location, it),
                            isTrueNorth = true,
                            angularDiameter = 0.5f
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

                val phase = astro.getMoonPhase(time.toLocalDate())
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
                        onMoonFocus(time)
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
                        onSunFocus(time)
                    }
                )

                val sunPointsToDraw = if (drawBelowHorizon) {
                    listOf(sunPositions)
                } else {
                    getMarkersAboveHorizon(sunPositions)
                }

                val moonPointsToDraw = if (drawBelowHorizon) {
                    listOf(moonPositions)
                } else {
                    getMarkersAboveHorizon(moonPositions)
                }


                // TODO: The line should be drawn to the horizon
                sunLineLayer.setLines(sunPointsToDraw.map { markers -> markers.map { it.point } })
                moonLineLayer.setLines(moonPointsToDraw.map { markers -> markers.map { it.point } })
                sunLayer.setMarkers(sunPointsToDraw.flatten())
                moonLayer.setMarkers(moonPointsToDraw.flatten())

                // TODO: Should the sun and moon be drawn below the horizon?
                currentSunLayer.setMarkers(listOf(sun))
                currentMoonLayer.setMarkers(listOf(moon))
            }
        }
    }

    private fun getMarkersAboveHorizon(points: List<ARMarker>): List<List<ARMarker>> {
        val lines = mutableListOf<List<ARMarker>>()
        var currentLine = mutableListOf<ARMarker>()
        points.forEach {
            val point = it.point as SphericalARPoint
            if (point.position.elevation > 0) {
                currentLine.add(it)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = mutableListOf()
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
}