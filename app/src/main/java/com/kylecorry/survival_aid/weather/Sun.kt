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

package com.kylecorry.survival_aid.weather

import java.time.Instant
import android.text.format.DateUtils
import com.kylecorry.survival_aid.navigator.gps.Coordinate
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.math.*


object Sun {

    private val DEGREES_TO_RADIANS = (Math.PI / 180.0f).toFloat()
    private val J0 = 0.0009f
    private val ALTIDUTE_CORRECTION_CIVIL_TWILIGHT = -0.104719755f
    private val C1 = 0.0334196f
    private val C2 = 0.000349066f
    private val C3 = 0.000005236f
    private val OBLIQUITY = 0.40927971f
    private val UTC_2000 = 946728000000L


    fun getSunrise(coordinate: Coordinate, day: Instant = Instant.now()): LocalDateTime {
        val daysSince2000 = Duration.between(Instant.ofEpochMilli(946728000000L), day).toDays()
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
        val latRad = coordinate.latitude * DEGREES_TO_RADIANS
        val cosHourAngle =
            (sin(ALTIDUTE_CORRECTION_CIVIL_TWILIGHT.toDouble()) - sin(latRad) * sin(
                solarDec
            )) / (cos(latRad) * cos(solarDec))
        // The day or night never ends for the given date and location, if this value is out of
        // range.
        if (cosHourAngle >= 1) {
            return LocalDateTime.now()
        } else if (cosHourAngle <= -1) {
            return LocalDateTime.now()
        }
        val hourAngle = (acos(cosHourAngle) / (2 * Math.PI)).toFloat()
//        sunset = Math.round((solarTransitJ2000 + hourAngle) * DateUtils.DAY_IN_MILLIS) + UTC_2000
        val instant = Instant.ofEpochMilli(((solarTransitJ2000 - hourAngle) * DateUtils.DAY_IN_MILLIS).roundToLong() + UTC_2000)
        return LocalDateTime.ofInstant(instant, OffsetDateTime.now().offset)
    }

    fun getSunset(coordinate: Coordinate, day: Instant = Instant.now()): LocalDateTime {
        val daysSince2000 = Duration.between(Instant.ofEpochMilli(946728000000L), day).toDays()
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
        val latRad = coordinate.latitude * DEGREES_TO_RADIANS
        val cosHourAngle =
            (sin(ALTIDUTE_CORRECTION_CIVIL_TWILIGHT.toDouble()) - sin(latRad) * sin(
                solarDec
            )) / (cos(latRad) * cos(solarDec))
        // The day or night never ends for the given date and location, if this value is out of
        // range.
        if (cosHourAngle >= 1) {
            return LocalDateTime.now()
        } else if (cosHourAngle <= -1) {
            return LocalDateTime.now()
        }
        val hourAngle = (acos(cosHourAngle) / (2 * Math.PI)).toFloat()
        val instant = Instant.ofEpochMilli(((solarTransitJ2000 + hourAngle) * DateUtils.DAY_IN_MILLIS).roundToLong() + UTC_2000)
        return LocalDateTime.ofInstant(instant, OffsetDateTime.now().offset)
    }

}