package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.fromColorTemperature
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.astronomy.ui.format.PlanetMapper
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.SphericalARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARLine
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARMarker
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.ui.CanvasBitmap
import com.kylecorry.trail_sense.tools.augmented_reality.ui.CanvasCircle
import com.kylecorry.trail_sense.tools.navigation.ui.DrawerBitmapLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

class ARAstronomyLayer(
    private val drawBelowHorizon: Boolean,
    private val drawStars: Boolean,
    private val onSunFocus: (time: ZonedDateTime) -> Boolean,
    private val onMoonFocus: (time: ZonedDateTime, phase: MoonPhase) -> Boolean,
    private val onStarFocus: (star: Star) -> Boolean,
    private val onPlanetFocus: (planet: Planet) -> Boolean,
) : ARLayer {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val runner = CoroutineQueueRunner()

    private val lineAlpha = 30
    private val lineThickness = 1f

    private val lineLayer = ARLineLayer()
    private val sunLayer = ARMarkerLayer()
    private val currentSunLayer = ARMarkerLayer()

    private val moonLayer = ARMarkerLayer()
    private val currentMoonLayer = ARMarkerLayer()

    private val starLayer = ARMarkerLayer()
    private val planetLayer = ARMarkerLayer()
    private var planetMapper: PlanetMapper? = null

    private val astro = AstronomyService()

    private var bitmapLoader: DrawerBitmapLoader? = null

    private val hooks = Hooks()
    private val triggers = HookTriggers()

    private val updateFrequency = Duration.ofMinutes(1)
    private val updateDistance = Distance.meters(1000f)

    var timeOverride: ZonedDateTime? = null

    override suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        val location = view.location
        if (planetMapper == null) {
            planetMapper = PlanetMapper(view.context)
        }

        hooks.effect(
            "positions",
            timeOverride,
            triggers.frequency("positions", updateFrequency),
            triggers.distance("positions", location, updateDistance, highAccuracy = false)
        ) {
            updatePositions(drawer, location, timeOverride ?: ZonedDateTime.now())
        }

        lineLayer.update(drawer, view)
        sunLayer.update(
            drawer,
            view
        )
        moonLayer.update(drawer, view)
        currentSunLayer.update(drawer, view)
        currentMoonLayer.update(drawer, view)
        starLayer.update(drawer, view)
        planetLayer.update(drawer, view)
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        lineLayer.draw(drawer, view)
        // NOTE: The moon renders in front of the sun, but the focus/click order prioritizes the sun
        sunLayer.draw(drawer, view)
        moonLayer.draw(drawer, view)

        // Only draw the current position when the time is today (this will change in the future)
        if (timeOverride == null || timeOverride?.toLocalDate() == LocalDate.now()) {
            currentSunLayer.draw(drawer, view)
            currentMoonLayer.draw(drawer, view)
            planetLayer.draw(drawer, view)
            starLayer.draw(drawer, view)
        }
    }

    override fun invalidate() {
        lineLayer.invalidate()
        moonLayer.invalidate()
        sunLayer.invalidate()
        currentMoonLayer.invalidate()
        currentSunLayer.invalidate()
        starLayer.invalidate()
        planetLayer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return currentSunLayer.onClick(drawer, view, pixel) ||
                currentMoonLayer.onClick(drawer, view, pixel) ||
                sunLayer.onClick(drawer, view, pixel) ||
                moonLayer.onClick(drawer, view, pixel) ||
                planetLayer.onClick(drawer, view, pixel) ||
                starLayer.onClick(drawer, view, pixel)
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        return currentSunLayer.onFocus(drawer, view) ||
                currentMoonLayer.onFocus(drawer, view) ||
                sunLayer.onFocus(drawer, view) ||
                moonLayer.onFocus(drawer, view) ||
                planetLayer.onFocus(drawer, view) ||
                starLayer.onFocus(drawer, view)
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
                    Color.WHITE.withAlpha(60)
                )

                val moonAfterPathObject = CanvasCircle(
                    Color.WHITE.withAlpha(200)
                )

                val sunBeforePathObject = CanvasCircle(
                    AppColor.Yellow.color.withAlpha(60)
                )

                val sunAfterPathObject = CanvasCircle(
                    AppColor.Yellow.color.withAlpha(200)
                )

                val moonPathTimes = astro.getMoonAboveHorizonTimes(location, time)
                val sunPathTimes = astro.getSunAboveHorizonTimes(location, time)

                val moonPositions = if (moonPathTimes != null) {
                    Time.getReadings(
                        moonPathTimes.start.minus(granularity),
                        moonPathTimes.end.plus(granularity),
                        granularity
                    ) {
                        val obj = if (it.isBefore(time)) {
                            moonBeforePathObject
                        } else {
                            moonAfterPathObject
                        }

                        val phase = Astronomy.getMoonPhase(it)

                        ARMarker(
                            SphericalARPoint(
                                astro.getMoonAzimuth(location, it).value,
                                astro.getMoonAltitude(location, it),
                                isTrueNorth = true,
                                angularDiameter = 0.5f
                            ),
                            canvasObject = obj,
                            onFocusedFn = {
                                onMoonFocus(it, phase)
                            }
                        )
                    }.map { it.value }
                } else {
                    emptyList()
                }

                val sunPositions = if (sunPathTimes != null) {
                    Time.getReadings(
                        sunPathTimes.start.minus(granularity),
                        sunPathTimes.end.plus(granularity),
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
                } else {
                    emptyList()
                }

                val moonAltitude = astro.getMoonAltitude(location)
                val moonAzimuth = astro.getMoonAzimuth(location).value

                val sunAltitude = astro.getSunAltitude(location)
                val sunAzimuth = astro.getSunAzimuth(location).value

                val phase = Astronomy.getMoonPhase(time)
                val moonIconId = MoonPhaseImageMapper().getPhaseImage(phase.phase)
                val moonImageSize = drawer.dp(24f).toInt()
                val moonBitmap = bitmapLoader?.load(moonIconId, moonImageSize)
                val moonTilt = astro.getMoonTilt(location, time)

                val moon = ARMarker(
                    SphericalARPoint(
                        moonAzimuth,
                        moonAltitude,
                        isTrueNorth = true,
                        angularDiameter = 2f
                    ),
                    canvasObject = moonBitmap?.let { CanvasBitmap(moonBitmap, rotation = moonTilt) }
                        ?: CanvasCircle(Color.WHITE),
                    onFocusedFn = {
                        onMoonFocus(time, phase)
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

                val sunLines = sunPointsToDraw.map { markers ->
                    ARLine(
                        markers.map { it.point },
                        AppColor.Yellow.color.withAlpha(lineAlpha),
                        lineThickness,
                        ARLine.ThicknessUnits.Angle,
                    )
                }

                val moonLines = moonPointsToDraw.map { markers ->
                    ARLine(
                        markers.map { it.point },
                        Color.WHITE.withAlpha(lineAlpha),
                        lineThickness,
                        ARLine.ThicknessUnits.Angle,
                    )
                }

                updateStarLayer(location, time)
                updatePlanetLayer(location, time, drawer)

                lineLayer.setLines(sunLines + moonLines)
                sunLayer.setMarkers(sunPointsToDraw.flatten())
                moonLayer.setMarkers(moonPointsToDraw.flatten())

                // The sun and moon can be drawn below the horizon
                currentSunLayer.setMarkers(listOf(sun))
                currentMoonLayer.setMarkers(listOf(moon))
            }
        }
    }

    private suspend fun updateStarLayer(location: Coordinate, time: ZonedDateTime) = onDefault {
        val starMarkers = if (drawStars) {
            val stars = astro.getVisibleStars(location, time)
            stars.map {
                ARMarker(
                    SphericalARPoint(
                        it.second.first.value,
                        it.second.second,
                        isTrueNorth = true,
                        angularDiameter = map(
                            -it.first.magnitude,
                            -2f,
                            1.5f,
                            0.4f,
                            0.8f,
                            true
                        )
                    ),
                    canvasObject = CanvasCircle(
                        Colors.fromColorTemperature(
                            Astronomy.getColorTemperature(
                                it.first
                            )
                        )
                    ),
                    onFocusedFn = {
                        onStarFocus(it.first)
                    }
                )
            }
        } else {
            emptyList()
        }

        starLayer.setMarkers(starMarkers)
    }

    private suspend fun updatePlanetLayer(
        location: Coordinate,
        time: ZonedDateTime,
        drawer: ICanvasDrawer
    ) = onDefault {
        val markers = if (drawStars) {
            val planets = astro.getVisiblePlanets(location, time)
            planets.mapNotNull {
                val resId = planetMapper?.getImage(it.first) ?: return@mapNotNull null
                val bitmap = bitmapLoader?.load(
                    resId,
                    drawer.dp(24f).toInt()
                ) ?: return@mapNotNull null
                ARMarker(
                    SphericalARPoint(
                        it.second.azimuth.value,
                        it.second.altitude,
                        isTrueNorth = true,
                        angularDiameter = map(
                            -(it.second.visualMagnitude ?: 0f),
                            -2f,
                            1.5f,
                            0.4f,
                            0.8f,
                            true
                        )
                    ),
                    canvasObject = CanvasBitmap(bitmap),
                    onFocusedFn = {
                        onPlanetFocus(it.first)
                    }
                )
            }
        } else {
            emptyList()
        }

        starLayer.setMarkers(markers)
    }

    private fun getMarkersAboveHorizon(points: List<ARMarker>): List<List<ARMarker>> {
        val lines = mutableListOf<List<ARMarker>>()
        var currentLine = mutableListOf<ARMarker>()
        points.forEach {
            val point = it.point as SphericalARPoint
            // TODO: This isn't efficient
            if (point.coordinate.elevation > 0) {
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