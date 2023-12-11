package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.sol.math.filters.IFilter

class FilteredBarometer(
    private val barometer: IBarometer,
    private val minimumReadingCount: Int = 1,
    private val filterProvider: (initialValue: Float) -> IFilter
) : AbstractSensor(), IBarometer {

    private var filter: IFilter? = null
    private var filteredPressure: Float? = null
    private var readingCount = 0

    override fun startImpl() {
        filter = null
        filteredPressure = null
        readingCount = 0
        barometer.start(this::onUpdate)
    }

    override fun stopImpl() {
        barometer.stop(this::onUpdate)
    }

    private fun onUpdate(): Boolean {
        if (!barometer.hasValidReading || barometer.pressure == 0f){
            return true
        }

        readingCount++

        if (filter == null){
            filter = filterProvider(barometer.pressure)
        }

        filteredPressure = filter?.filter(barometer.pressure)

        if (readingCount < minimumReadingCount){
            return true
        }

        notifyListeners()
        return true
    }

    override val hasValidReading: Boolean
        get() = filteredPressure != null && readingCount >= minimumReadingCount

    override val pressure: Float
        get() = filteredPressure ?: 0f
}