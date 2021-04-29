package com.kylecorry.trail_sense.astronomy.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.sensors.read
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class AstronomyContextualService(private val context: Context) {
    private val foregroundGPS by lazy { SensorService(context).getGPS(false) }
    private val backgroundGPS by lazy { SensorService(context).getGPS(true) }
    private val prefs by lazy { UserPreferences(context) }

    private val astronomyService = AstronomyService()

    suspend fun isSunUp(isBackground: Boolean = false, useCache: Boolean = true): Boolean? {
        val location = getLocation(isBackground, useCache)

        if (location == Coordinate.zero) {
            return null
        }

        return withContext(Dispatchers.Default) {
            astronomyService.isSunUp(location)
        }
    }

    suspend fun getNextSunset(isBackground: Boolean = false, useCache: Boolean = true): LocalDateTime? {
        val location = getLocation(isBackground, useCache)

        if (location == Coordinate.zero) {
            return null
        }

        return withContext(Dispatchers.Default) {
            astronomyService.getNextSunset(location, prefs.astronomy.sunTimesMode)
        }
    }

    suspend fun getNextSunrise(isBackground: Boolean = false, useCache: Boolean = true): LocalDateTime? {
        val location = getLocation(isBackground, useCache)

        if (location == Coordinate.zero) {
            return null
        }

        return withContext(Dispatchers.Default) {
            astronomyService.getNextSunrise(location, prefs.astronomy.sunTimesMode)
        }
    }

    private suspend fun getLocation(background: Boolean, useCache: Boolean): Coordinate {
        val gps = getGPS(background)

        return if (!useCache) {
            withContext(Dispatchers.IO) {
                gps.read()
            }
            gps.location
        } else {
            gps.location
        }
    }

    private fun getGPS(background: Boolean): IGPS {
        return if (background) {
            backgroundGPS
        } else {
            foregroundGPS
        }
    }

}