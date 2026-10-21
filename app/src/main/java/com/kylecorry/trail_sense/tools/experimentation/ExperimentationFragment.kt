package com.kylecorry.trail_sense.tools.experimentation

import android.widget.Button
import android.widget.TextView
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useBackgroundMemo
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugin.sample.domain.Forecast
import com.kylecorry.trail_sense.plugin.sample.service.WeatherForecastService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useLocation
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import java.time.Duration

class ExperimentationFragment : TrailSenseReactiveFragment(R.layout.fragment_experimentation) {
    override fun update() {
//        useWormGrunting()
//        useSamplePlugin()
        useCaveNavigation()
    }

    private fun useCaveNavigation() {
        val text = useView<TextView>(R.id.text)
        val text2 = useView<TextView>(R.id.text2)
        val button = useView<Button>(R.id.button)
        val pathView = useView<CavePathView>(R.id.cave_path)

        val sensors = useService<SensorService>()
        val prefs = useService<UserPreferences>()
        val formatter = useService<FormatService>()
        val (location, _) = useLocation(SensorSubsystem.SensorRefreshPolicy.Cache)
        val (running, setRunning) = useState(false)
        val (tick, setTick) = useState(0)

        val reckoner = useMemo { DeadReckoner(prefs.pedometer.stepLength.meters().value) }
        val pedometer = useMemo(sensors) { sensors.getPedometer() }
        val compass = useMemo(sensors) { sensors.getCompass() }
        val barometer = useMemo(sensors) { sensors.getBarometer() }

        useEffect(pathView, reckoner) { pathView.reckoner = reckoner }

        useEffect(button, running, location) {
            button.text = if (running) "Stop" else "Start from last GPS"
            button.setOnClickListener {
                if (!running) {
                    reckoner.declination = Geophysics.getGeomagneticDeclination(location)
                    reckoner.reset(location)
                }
                setRunning(!running)
            }
        }

        useEffectWithCleanup(running, pedometer, compass, barometer, reckoner) {
            var lastSteps = -1
            val onStep = {
                if (lastSteps >= 0) {
                    reckoner.addSteps(pedometer.steps - lastSteps)
                }
                lastSteps = pedometer.steps
                setTick(reckoner.steps)
                true
            }
            val onHeading = {
                reckoner.addHeading(compass.bearing.value)
                true
            }
            val onPressure = {
                reckoner.addPressure(barometer.pressure)
                true
            }

            if (running) {
                pedometer.start(onStep)
                compass.start(onHeading)
                barometer.start(onPressure)
            }

            return@useEffectWithCleanup {
                pedometer.stop(onStep)
                compass.stop(onHeading)
                barometer.stop(onPressure)
            }
        }

        useEffect(text, text2, tick, running, location) {
            val p = reckoner.position
            text.text = if (reckoner.path.isEmpty()) {
                "Start: ${formatter.formatLocation(location)}"
            } else {
                "%d steps, %.0f m walked\nE %.1f  N %.1f  Up %.1f m\nBack to start: %.0f m at %.0f°".format(
                    reckoner.steps, reckoner.distanceWalked, p.east, p.north, p.up,
                    reckoner.distanceToStart, reckoner.bearingToStart
                )
            }
            text2.text = if (reckoner.path.isEmpty()) {
                ""
            } else {
                "${formatter.formatLocation(reckoner.estimatedCoordinate)}\nCompass quality: ${compass.quality}"
            }
        }
    }

    private fun useSamplePlugin() {
        val text = useView<TextView>(R.id.text)
        val text2 = useView<TextView>(R.id.text2)
        val button = useView<Button>(R.id.button)

        val (isLoading, setIsLoading) = useState(false)
        val (weather, setWeather) = useState<Forecast?>(null)
        val (location, _) = useLocation()
        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()
        val markdown = useService<MarkdownService>()
        val pluginSubsystem = useService<PluginSubsystem>()

        val plugins = useBackgroundMemo(pluginSubsystem) {
            pluginSubsystem.getPluginResourceServiceDetails()
        }

        val weatherService = useMemo(plugins) {
            val plugin = plugins?.firstOrNull { it.features.weather.isNotEmpty() }
            if (plugin != null) {
                WeatherForecastService(plugin)
            } else {
                null
            }
        }

        useEffect(button, weatherService, location) {
            button.setOnClickListener {
                inBackground {
                    setIsLoading(true)
                    tryOrLog {
                        setWeather(weatherService?.getWeather(location))
                    }
                    setIsLoading(false)
                }
            }
        }

        useEffect(text, text2, weather, isLoading, plugins) {
            val weatherText = if (isLoading) {
                "Loading"
            } else if (weather == null) {
                "No data"
            } else {
                formatter.join(
                    formatter.formatTemperature(
                        Temperature.celsius(weather.current.temperature ?: 0f)
                            .convertTo(prefs.temperatureUnits)
                    ),
                    formatter.formatPercentage(weather.current.humidity ?: 0f),
                    formatter.formatSpeed(
                        Speed.from(
                            weather.current.windSpeed ?: 0f,
                            DistanceUnits.Kilometers,
                            TimeUnits.Hours
                        ).convertTo(
                            DistanceUnits.Meters,
                            TimeUnits.Seconds
                        ).value
                    ),
                    formatter.formatWeather(weather.current.weather),
                    separator = FormatService.Separator.NewLine
                )
            }

            text.text = "$weatherText\n\n${plugins?.joinToString("\n\n")}"
            markdown.setMarkdown(text2, weather?.citation ?: "")
        }
    }

    private fun useWormGrunting() {
        val context = useAndroidContext()
        val haptics = useMemo {
            HapticSubsystem.getInstance(context)
        }

        useEffectWithCleanup {
            haptics.interval(Duration.ofMillis(700), Duration.ofMillis(900))
            return@useEffectWithCleanup {
                haptics.off()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        resetHooks()
    }
}
