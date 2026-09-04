package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.AltitudeCorrection
import com.kylecorry.trail_sense.shared.UserPreferences
import kotlinx.coroutines.runBlocking

class MeanSeaLevelGPSModule : GPSModule {
    private val userPrefs = getAppService<UserPreferences>()
    private var mslOffset = 0f

    @Volatile
    private var geoidOffset = 0f

    @Volatile
    private var geoidLocation: Coordinate? = null
    private var lastData: ModularGPSData? = null

    private val geoidRunner = CoroutineQueueRunner()
    private val geoidTask = BackgroundTask {
        geoidRunner.enqueue {
            val currentLocation = lastData?.location ?: return@enqueue
            geoidOffset = AltitudeCorrection.getGeoid(currentLocation)
            geoidLocation = currentLocation
        }
    }


    override fun update(
        previousData: ModularGPSData,
        newData: ModularGPSData
    ) {
        lastData = newData
        val newMSLOffset = newData.altitude - (newData.mslAltitude ?: newData.altitude)
        if (newMSLOffset != 0f) {
            mslOffset = newMSLOffset
        }

        newData.altitude -= getGeoidOffset(newData.location)
    }

    private fun getGeoidOffset(location: Coordinate): Float {
        if (userPrefs.useNMEA && mslOffset != 0f) {
            return mslOffset
        }

        val lastLocation = geoidLocation

        if (lastLocation == null) {
            // This is not ideal, but an offset is needed (and this service caches it)
            geoidOffset = runBlocking { AltitudeCorrection.getGeoid(location) }
            geoidLocation = location
        } else if (!AltitudeCorrection.isSameGeoid(lastLocation, location)) {
            geoidTask.start()
        }

        return geoidOffset
    }

    override fun start(data: ModularGPSData) {
        lastData = data
        // Load the offset for the last known location so the first fix doesn't have to wait on it
        geoidTask.start()
    }

    override fun stop(data: ModularGPSData) {
        geoidTask.stop()
        geoidRunner.cancel()
    }
}
