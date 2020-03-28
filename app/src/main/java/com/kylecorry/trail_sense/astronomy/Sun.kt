package com.kylecorry.trail_sense.astronomy

// Ported from AOSP's TwilightCalculate file
/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.text.format.DateUtils
import com.kylecorry.trail_sense.models.Coordinate
import com.kylecorry.trail_sense.toZonedDateTime
import java.time.*
import kotlin.math.*


object Sun {

    private const val J0 = 0.0009f
    private const val ALTITUDE_CORRECTION_CIVIL_TWILIGHT = -0.104719755f
    private const val C1 = 0.0334196f
    private const val C2 = 0.000349066f
    private const val C3 = 0.000005236f
    private const val OBLIQUITY = 0.40927971f
    private const val UTC_2000 = 946728000000L


    fun getSunrise(coordinate: Coordinate, day: ZonedDateTime = ZonedDateTime.now()): LocalTime {
        val daysSince2000 = Duration.between(Instant.ofEpochMilli(UTC_2000), day.toInstant()).toDays()
        val meanAnomaly = 6.240059968f + daysSince2000 * 0.01720197f
        val trueAnomaly =
            meanAnomaly.toDouble() + C1 * sin(meanAnomaly.toDouble()) + C2 * sin((2 * meanAnomaly).toDouble()) + C3 * sin(
                (3 * meanAnomaly).toDouble()
            )
        // ecliptic longitude
        val solarLng = trueAnomaly + 1.796593063 + Math.PI
        // solar transit in days since 2000
        val arcLongitude = -coordinate.longitude / 360
        val n = (daysSince2000.toDouble() - J0.toDouble() - arcLongitude).roundToLong().toFloat()
        val solarTransitJ2000 =
            (n.toDouble() + J0.toDouble() + arcLongitude + 0.0053 * sin(meanAnomaly.toDouble())
                    + -0.0069 * sin(2 * solarLng))
        // declination of sun
        val solarDec = asin(sin(solarLng) * sin(OBLIQUITY.toDouble()))
        val latRad = Math.toRadians(coordinate.latitude)
        val cosHourAngle =
            (sin(ALTITUDE_CORRECTION_CIVIL_TWILIGHT.toDouble()) - sin(latRad) * sin(
                solarDec
            )) / (cos(latRad) * cos(solarDec))
        // The day or night never ends for the given date and location, if this value is out of
        // range.
        if (cosHourAngle >= 1) {
            return LocalTime.now()
        } else if (cosHourAngle <= -1) {
            return LocalTime.now()
        }
        val hourAngle = (acos(cosHourAngle) / (2 * Math.PI)).toFloat()
        val instant = Instant.ofEpochMilli(((solarTransitJ2000 - hourAngle) * DateUtils.DAY_IN_MILLIS).roundToLong() + UTC_2000)
        return instant.toZonedDateTime().toLocalTime()
    }

    fun getSunset(coordinate: Coordinate, day: ZonedDateTime = ZonedDateTime.now()): LocalTime {
        val daysSince2000 = Duration.between(Instant.ofEpochMilli(UTC_2000), day.toInstant()).toDays()
        val meanAnomaly = 6.240059968f + daysSince2000 * 0.01720197f
        val trueAnomaly =
            meanAnomaly.toDouble() + C1 * sin(meanAnomaly.toDouble()) + C2 * sin((2 * meanAnomaly).toDouble()) + C3 * sin(
                (3 * meanAnomaly).toDouble()
            )
        // ecliptic longitude
        val solarLng = trueAnomaly + 1.796593063 + Math.PI
        // solar transit in days since 2000
        val arcLongitude = -coordinate.longitude / 360
        val n = (daysSince2000.toDouble() - J0.toDouble() - arcLongitude).roundToLong().toFloat()
        val solarTransitJ2000 =
            (n.toDouble() + J0.toDouble() + arcLongitude + 0.0053 * sin(meanAnomaly.toDouble())
                    + -0.0069 * sin(2 * solarLng))
        // declination of sun
        val solarDec = asin(sin(solarLng) * sin(OBLIQUITY.toDouble()))
        val latRad = Math.toRadians(coordinate.latitude)
        val cosHourAngle =
            (sin(ALTITUDE_CORRECTION_CIVIL_TWILIGHT.toDouble()) - sin(latRad) * sin(
                solarDec
            )) / (cos(latRad) * cos(solarDec))
        // The day or night never ends for the given date and location, if this value is out of
        // range.
        if (cosHourAngle >= 1) {
            return LocalTime.now()
        } else if (cosHourAngle <= -1) {
            return LocalTime.now()
        }
        val hourAngle = (acos(cosHourAngle) / (2 * Math.PI)).toFloat()
        val instant = Instant.ofEpochMilli(((solarTransitJ2000 + hourAngle) * DateUtils.DAY_IN_MILLIS).roundToLong() + UTC_2000)
        return instant.toZonedDateTime().toLocalTime()
    }

}