package com.kylecorry.trail_sense.astronomy.sun

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
import com.kylecorry.trail_sense.shared.Coordinate
import com.kylecorry.trail_sense.shared.toZonedDateTime
import java.time.*
import kotlin.math.*

abstract class BaseSunTimesCalculator(private val sunAngleDeg: Float) :
    ISunTimesCalculator {

    override fun calculate(coordinate: Coordinate, date: LocalDate): SunTimes {
        val current = date.atTime(12, 0, 0)

        val daysSince2000 = Duration.between(JANUARY_1_2000, current).toDays()
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
            (sin(Math.toRadians(sunAngleDeg.toDouble())) - sin(latRad) * sin(
                solarDec
            )) / (cos(latRad) * cos(solarDec))
        // The day or night never ends for the given date and location, if this value is out of
        // range.
        if (cosHourAngle >= 1) {
            return SunTimes.unknown(
                date
            )
        } else if (cosHourAngle <= -1) {
            return SunTimes.unknown(
                date
            )
        }
        val hourAngle = (acos(cosHourAngle) / (2 * Math.PI)).toFloat()
        val up = Instant.ofEpochMilli(
            ((solarTransitJ2000 - hourAngle) * DateUtils.DAY_IN_MILLIS).roundToLong() + JANUARY_1_2000.toEpochSecond(
                ZoneOffset.UTC
            ) * 1000
        )
        val down = Instant.ofEpochMilli(
            ((solarTransitJ2000 + hourAngle) * DateUtils.DAY_IN_MILLIS).roundToLong() + JANUARY_1_2000.toEpochSecond(
                ZoneOffset.UTC
            ) * 1000
        )

        val upTime = up.toZonedDateTime().toLocalDateTime()
        val downTime = down.toZonedDateTime().toLocalDateTime()

        return SunTimes(upTime, downTime)
    }

    companion object {
        private const val J0 = 0.0009f
        private const val C1 = 0.0334196f
        private const val C2 = 0.000349066f
        private const val C3 = 0.000005236f
        private const val OBLIQUITY = 0.40927971f
        private val JANUARY_1_2000 = LocalDateTime.of(2000, Month.JANUARY, 1, 12, 0)
    }

}