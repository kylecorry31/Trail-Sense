package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.GeoidService
import com.kylecorry.trail_sense.shared.UserPreferences
import kotlinx.coroutines.runBlocking

class MeanSeaLevelGPSModule(
    private val userPrefs: UserPreferences = getAppService(),
    private val geoidService: GeoidService = getAppService()
) : GPSModule {
    private var mslOffset = 0f

    @Volatile
    private var geoidOffset = 0f

    @Volatile
    private var geoidLocation: Coordinate? = null

    @Volatile
    private var lastLocation: Coordinate? = null

    private val geoidRunner = CoroutineQueueRunner()
    private val geoidTask = BackgroundTask {
        geoidRunner.enqueue {
            val currentLocation = lastLocation ?: return@enqueue
            geoidOffset = geoidService.getGeoid(currentLocation)
            geoidLocation = currentLocation
        }
    }


    override fun update(
        previousData: ModularGPSData,
        newData: ModularGPSData
    ): Boolean {
        lastLocation = newData.location
        val newMSLOffset = newData.altitude - (newData.mslAltitude ?: newData.altitude)
        if (newMSLOffset != 0f) {
            mslOffset = newMSLOffset
        }

        newData.altitude -= getGeoidOffset(newData.location)

        return true
    }

    private fun getGeoidOffset(location: Coordinate): Float {
        if (userPrefs.useNMEA && mslOffset != 0f) {
            return mslOffset
        }

        val lastGeoidLocation = geoidLocation

        if (lastGeoidLocation == null) {
            // This is not ideal, but an offset is needed (and this service caches it)
            geoidOffset = runBlocking { geoidService.getGeoid(location) }
            geoidLocation = location
        } else if (!geoidService.isSameGeoid(lastGeoidLocation, location)) {
            geoidTask.start()
        }

        return geoidOffset
    }

    override fun start(data: ModularGPSData) {
        lastLocation = data.location
        // Load the offset for the last known location so the first fix doesn't have to wait on it
        geoidTask.start()
    }

    override fun stop(data: ModularGPSData) {
        geoidTask.stop()
        geoidRunner.cancel()
    }
}
