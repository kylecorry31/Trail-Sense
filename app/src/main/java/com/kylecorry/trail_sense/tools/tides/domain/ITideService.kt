package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.units.Reading
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

interface ITideService {
    fun getTides(table: TideTable, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Tide>
    fun getWaterLevel(table: TideTable, time: ZonedDateTime): Float
    fun getWaterLevels(table: TideTable, date: LocalDate): List<Reading<Float>>
    fun getRange(table: TideTable): Range<Float>
    fun isWithinTideTable(table: TideTable, time: ZonedDateTime = ZonedDateTime.now()): Boolean
    fun getCurrentTide(table: TideTable, time: ZonedDateTime = ZonedDateTime.now()): TideType?
    fun isRising(table: TideTable, time: ZonedDateTime = ZonedDateTime.now()): Boolean
}