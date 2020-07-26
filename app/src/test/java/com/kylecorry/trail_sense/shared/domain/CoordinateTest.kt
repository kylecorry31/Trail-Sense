package com.kylecorry.trail_sense.shared.domain

import org.junit.Assert.*
import org.junit.Test

class CoordinateTest {

    @Test
    fun canConvertToString(){
        assertEquals("10°2'5.0\" N, 77°30'30.0\" E", Coordinate(10.03472, 77.508333).toString())
        assertEquals("10°2'5.0\" S, 77°30'30.0\" E", Coordinate(-10.03472, 77.508333).toString())
        assertEquals("10°2'5.0\" N, 77°30'30.0\" W", Coordinate(10.03472, -77.508333).toString())
        assertEquals("10°2'5.0\" S, 77°30'30.0\" W", Coordinate(-10.03472, -77.508333).toString())
    }

    @Test
    fun canConvertToFormattedString(){
        assertEquals("10°2'5.0\" N    77°30'30.0\" E", Coordinate(10.03472, 77.508333).getFormattedString())
        assertEquals("10°2'5.0\" S    77°30'30.0\" E", Coordinate(-10.03472, 77.508333).getFormattedString())
        assertEquals("10°2'5.0\" N    77°30'30.0\" W", Coordinate(10.03472, -77.508333).getFormattedString())
        assertEquals("10°2'5.0\" S    77°30'30.0\" W", Coordinate(-10.03472, -77.508333).getFormattedString())
    }

    @Test
    fun canParseLongitude(){
        val cases = listOf(
            // DMS
            Pair(10.03472, "10°2'5\" E"),
            Pair(-10.03472, "10°2'5\" W"),
//            Pair(10.03472, "10°2'5\""),
//            Pair(-10.03472, "-10°2'5\""),
            Pair(10.03472, "10°2'5\" e"),
            Pair(-10.03472, "10°2'5\" w"),
            Pair(10.03472, "10°2'5\"E"),
            Pair(-10.03472, "10°2'5\"W"),
            Pair(10.03472, "10° 2' 5\" E"),
            Pair(-10.03472, "10° 2' 5\" W"),
            // DDM
            Pair (77.508333, "77°30.5' E"),
            Pair (-77.508333, "77°30.5' W"),
//            Pair (77.508333, "77°30.5'"),
//            Pair (-77.508333, "-77°30.5'"),
            Pair (77.508333, "77°30.5' e"),
            Pair (-77.508333, "77°30.5' w"),
            Pair (77.508333, "77°30.5'E"),
            Pair (-77.508333, "77°30.5'W"),
            Pair (77.508333, "77° 30.5' E"),
            Pair (-77.508333, "77° 30.5' W"),
            // Decimal
//            Pair(12.4, "12.4 E"),
//            Pair(-12.4, "12.4 W"),
            Pair(12.4, "12.4"),
            Pair(-12.4, "-12.4"),
            Pair(180.0, "180"),
            Pair(-180.0, "-180")
        )

        for (case in cases){
            assertEquals(case.first, Coordinate.parseLongitude(case.second)!!, 0.00001)
        }
    }

    @Test
    fun parseReturnsNullWhenInvalidLongitude(){
        val cases = listOf(
            "10°2'5 E",
            "10°2'5\" R",
            "10°25\" E",
            "102'5 E",
            "10°2'5 N",
            "10°2'5 S",
            "a10°2'5 E",
            "",
            "something",
            "181",
            "-181",
            "180°2'5\" E",
            "180°2' E")

        for (case in cases){
            assertNull(Coordinate.parseLongitude(case))
        }
    }

    @Test
    fun canParseLatitude(){
        val cases = listOf(
            // DMS
            Pair(10.03472, "10°2'5\" N"),
            Pair(-10.03472, "10°2'5\" S"),
//            Pair(10.03472, "10°2'5\""),
//            Pair(-10.03472, "-10°2'5\""),
            Pair(10.03472, "10°2'5\" n"),
            Pair(-10.03472, "10°2'5\" s"),
            Pair(10.03472, "10°2'5\"N"),
            Pair(-10.03472, "10°2'5\"S"),
            Pair(10.03472, "10° 2' 5\" N"),
            Pair(-10.03472, "10° 2' 5\" S"),
            // DDM
            Pair (77.508333, "77°30.5' N"),
            Pair (-77.508333, "77°30.5' S"),
//            Pair (77.508333, "77°30.5'"),
//            Pair (-77.508333, "-77°30.5'"),
            Pair (77.508333, "77°30.5' n"),
            Pair (-77.508333, "77°30.5' s"),
            Pair (77.508333, "77°30.5'N"),
            Pair (-77.508333, "77°30.5'S"),
            Pair (77.508333, "77° 30.5' N"),
            Pair (-77.508333, "77° 30.5' S"),
            // Decimal
//            Pair(12.4, "12.4 N"),
//            Pair(-12.4, "12.4 S"),
            Pair(12.4, "12.4"),
            Pair(-12.4, "-12.4"),
            Pair(90.0, "90"),
            Pair(-90.0, "-90")
        )

        for (case in cases){
            assertEquals(case.first, Coordinate.parseLatitude(case.second)!!, 0.00001)
        }
    }

    @Test
    fun parseReturnsNullWhenInvalidLatitude(){
        val cases = listOf(
            "10°2'5 N",
            "10°2'5\" R",
            "10°25\" N",
            "102'5 N",
            "10°2'5 E",
            "10°2'5 W",
            "a10°2'5 S",
            "",
            "something",
            "91",
            "-91",
            "90°2'5\" N",
            "90°2' N")

        for (case in cases){
            assertNull(Coordinate.parseLatitude(case))
        }
    }

}