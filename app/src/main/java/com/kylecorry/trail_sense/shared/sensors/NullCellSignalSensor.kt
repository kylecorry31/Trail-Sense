package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.signal.CellSignal
import com.kylecorry.andromeda.signal.ICellSignalSensor

class NullCellSignalSensor: AbstractSensor(), ICellSignalSensor {
    override val hasValidReading: Boolean
        get() = true
    override val signals: List<CellSignal>
        get() = listOf()

    private val intervalometer = Timer {
        notifyListeners()
    }

    override fun startImpl() {
        intervalometer.interval(20)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }
}