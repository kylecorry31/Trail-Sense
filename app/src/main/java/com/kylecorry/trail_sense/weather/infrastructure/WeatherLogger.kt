package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.trail_sense.weather.infrastructure.commands.MonitorWeatherCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration

class WeatherLogger(context: Context, private val interval: Duration, private val intialDelay: Duration = Duration.ZERO) {

    private val runner = ControlledRunner<Unit>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val command = MonitorWeatherCommand(context, false)
    private val timer = Timer {
        scope.launch {
            runner.joinPreviousOrRun {
                command.execute()
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