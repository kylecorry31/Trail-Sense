package com.kylecorry.trail_sense.tools.experimentation

import android.content.Context
import android.widget.Button
import android.widget.TextView
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugin.sample.domain.Forecast
import com.kylecorry.trail_sense.plugin.sample.service.SamplePluginService
import com.kylecorry.trail_sense.plugin.sample.service.WeatherForecastService
import com.kylecorry.trail_sense.plugins.plugins.PluginLoader
import com.kylecorry.trail_sense.plugins.plugins.Plugins
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.useBackgroundMemo2
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useLocation
import com.kylecorry.trail_sense.shared.extensions.usePauseEffect
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import java.io.Closeable
import java.time.Duration

class ExperimentationFragment : TrailSenseReactiveFragment(R.layout.fragment_experimentation) {
    override fun update() {
//        useWormGrunting()
        useSamplePlugin()
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
        val context = useAndroidContext()

        val service = usePluginService(
            Plugins.PLUGIN_SAMPLE,
            ::SamplePluginService
        )

        val finder = useMemo(context) {
            PluginLoader(context)
        }

        val plugins = useBackgroundMemo2(finder) {
            finder.getPluginResourceServices()
        }

        val weatherService = useMemo(plugins) {
            val plugin = plugins?.firstOrNull { it.features.weather.isNotEmpty() }
            if (plugin != null) {
                WeatherForecastService(context, plugin)
            } else {
                null
            }
        }

        val data = useBackgroundMemo2(service) {
            service?.ping()
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

        useEffect(text, text2, weather, service, isLoading, data) {
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
                        ).speed
                    ),
                    formatter.formatWeather(weather.current.weather),
                    separator = FormatService.Separator.NewLine
                )
            }

            text.text = "$weatherText\n\n$data\n\n${plugins?.joinToString("\n\n")}"
            markdown.setMarkdown(text2, weather?.citation ?: "")
        }
    }

    private fun <T : Closeable> usePluginService(
        pluginId: Long,
        serviceProvider: (context: Context) -> T
    ): T? {
        val context = useAndroidContext()

        // TODO: Retry on a timer
        val service = useMemo(pluginId) {
            if (Plugins.isPluginAvailable(context, pluginId)) {
                serviceProvider(context)
            } else {
                null
            }
        }

        usePauseEffect(service) {
            service?.close()
        }

        return service
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