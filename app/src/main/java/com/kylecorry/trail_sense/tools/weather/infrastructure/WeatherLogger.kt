package com.kylecorry.trail_sense.tools.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration

class WeatherLogger(
    context: Context,
    private val interval: Duration,
    private val intialDelay: Duration = Duration.ZERO,
    private val loadingIndicator: ILoadingIndicator
) {
    private val weather = WeatherSubsystem.getInstance(context)
    private val runner = CoroutineQueueRunner()
    private val timer = CoroutineTimer {
        onIO {
            runner.skipIfRunning {
                onMain { loadingIndicator.show() }
                weather.updateWeather()
                onMain { loadingIndicator.hide() }
            }
        }
    }

    fun start() {
        timer.interval(interval, intialDelay)
    }

    fun stop() {
        timer.stop()
        runner.cancel()
    }

}