package com.kylecorry.trail_sense.shared.domain

enum class BuiltInCoordinateFormat(val id: Int) {
    DecimalDegrees(1),
    DegreesDecimalMinutes(2),
    DegreesMinutesSeconds(3),
    UTM(4),
    MGRS(5),
    USNG(6),
    OSGB(7)
}