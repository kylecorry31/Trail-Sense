package com.kylecorry.trail_sense.shared.sensors.gps

interface GPSModule {
    /**
     * Applies this module to a candidate reading. Do not modify previousData, newData should be
     * updated with any changes.
     * @return false to reject the reading, which stops any remaining modules from running
     */
    fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean
    fun start(data: ModularGPSData) {}
    fun stop(data: ModularGPSData) {}
}
