package com.kylecorry.trail_sense.tools.astronomy.domain

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

class AstronomySubsystem(context: Context) {

    private val astronomyService = AstronomyService()
    private val location = LocationSubsystem.getInstance(context)
    private val prefs = UserPreferences(context)
    private val hooks = Hooks()
    private val hookTriggers = HookTriggers()

    val sun: SunDetails
        get() = hooks.memo(
            "sun",
            prefs.astronomy.sunTimesMode,
            hookTriggers.distance("sun", location.location, Distance.kilometers(1f)),
            hookTriggers.frequency("sun", Duration.ofMinutes(1))
        ) {
            val sunTimesMode = prefs.astronomy.sunTimesMode
            val location = location.location
            val times = astronomyService.getSunTimes(location, sunTimesMode, LocalDate.now())
            val azimuth = astronomyService.getSunAzimuth(location)
            val altitude = astronomyService.getSunAltitude(location)

            val nextSunrise = astronomyService.getNextSunrise(location, sunTimesMode)
            val nextSunset = astronomyService.getNextSunset(location, sunTimesMode)

            SunDetails(
                times.rise?.toLocalDateTime(),
                times.set?.toLocalDateTime(),
                times.transit?.toLocalDateTime(),
                altitude > 0,
                nextSunrise,
                nextSunset,
                altitude,
                azimuth
            )
        }

    val moon: MoonDetails
        get() = hooks.memo(
            "moon",
            hookTriggers.distance("moon", location.location, Distance.kilometers(1f)),
            hookTriggers.frequency("moon", Duration.ofMinutes(1))
        ) {
            val location = location.location
            val moonTimes = astronomyService.getMoonTimes(location, LocalDate.now())
            val phase = astronomyService.getCurrentMoonPhase()
            val altitude = astronomyService.getMoonAltitude(location)
            val azimuth = astronomyService.getMoonAzimuth(location)
            val tilt = astronomyService.getMoonTilt(location)

            MoonDetails(
                moonTimes.rise?.toLocalDateTime(),
                moonTimes.set?.toLocalDateTime(),
                moonTimes.transit?.toLocalDateTime(),
                altitude > 0,
                phase.phase,
                phase.illumination,
                altitude,
                azimuth,
                tilt
            )
        }

    fun getSunAltitudes(): List<Reading<Float>> {
        return if (!prefs.astronomy.centerSunAndMoon) {
            astronomyService.getSunAltitudes(location.location, LocalDate.now())
        } else {
            astronomyService.getCenteredSunAltitudes(location.location, ZonedDateTime.now())
        }
    }

    fun getMoonAltitudes(): List<Reading<Float>> {
        return if (!prefs.astronomy.centerSunAndMoon) {
            astronomyService.getMoonAltitudes(location.location, LocalDate.now())
        } else {
            astronomyService.getCenteredMoonAltitudes(location.location, ZonedDateTime.now())
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: AstronomySubsystem? = null

        @Synchronized
        fun getInstance(context: Context): AstronomySubsystem {
            if (instance == null) {
                instance = AstronomySubsystem(context.applicationContext)
            }
            return instance!!
        }
    }

}

