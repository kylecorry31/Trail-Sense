package com.kylecorry.trail_sense.shared.sensors.gps

interface GPSModule {
    /**
     * Do not modify previousData. newData should be updated with any changes.
     */
    fun update(previousData: ModularGPSData, newData: ModularGPSData)
    fun start(data: ModularGPSData) {}
    fun stop(data: ModularGPSData) {}
}
