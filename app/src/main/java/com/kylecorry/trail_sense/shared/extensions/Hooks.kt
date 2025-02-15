package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.useTopic
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.SensorService
import java.time.Duration

fun ReactiveComponent.useCoroutineQueue(): CoroutineQueueRunner {
    return useMemo {
        CoroutineQueueRunner()
    }
}

// Sensors

fun ReactiveComponent.useGPSSensor(frequency: Duration = Duration.ofMillis(20)): IGPS {
    val sensors = useService<SensorService>()
    return useMemo(sensors, frequency.seconds, frequency.nano) { sensors.getGPS(frequency) }
}

fun ReactiveComponent.useCellSignalSensor(removeUnregisteredSignals: Boolean = true): ICellSignalSensor {
    val sensors = useService<SensorService>()
    return useMemo(sensors, removeUnregisteredSignals) {
        sensors.getCellSignal(
            removeUnregisteredSignals
        )
    }
}

// Common sensor readings
fun AndromedaFragment.useLocation(frequency: Duration = Duration.ofMillis(20)): Coordinate {
    val gps = useGPSSensor(frequency)
    return useTopic(gps, gps.location) {
        it.location
    }
}