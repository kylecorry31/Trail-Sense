package com.kylecorry.trail_sense.tools.climate

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.test_utils.ManualOnlyTest
import com.kylecorry.trail_sense.tools.climate.infrastructure.precipitation.HistoricMonthlyPrecipitationRepo
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.HistoricMonthlyTemperatureRangeRepo
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ClimateNormalsExport {

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DependencyRegistry.addSingleton(FileSubsystem.getInstance(context))
    }

    @ManualOnlyTest
    @Test
    fun export() = runBlocking {
        val stations = mapOf(
            "PVD" to Coordinate(41.7219, -71.4325),
            "SBEG" to Coordinate(-3.038611, -60.049721),
            "YMHB" to Coordinate(-42.836111, 147.510278),
            "VTBS" to Coordinate(13.68084, 100.74768),
            "RPLL" to Coordinate(14.508647, 121.019581),
            "WIII" to Coordinate(-6.125567, 106.655897),
            "VOMM" to Coordinate(12.994414, 80.180517),
            "GOOY" to Coordinate(14.739708, -17.490225),
            "FKKD" to Coordinate(4.006081, 9.719481),
            "HAAB" to Coordinate(8.977889, 38.799319),
            "MROC" to Coordinate(9.993861, -84.208806),
            "MMMX" to Coordinate(19.436303, -99.072097),
            "SKBO" to Coordinate(4.701594, -74.146947),
            "YBCS" to Coordinate(-16.885833, 145.755278),
            "NFFN" to Coordinate(-17.755392, 177.443378),
            "FMMI" to Coordinate(-18.796936, 47.478869),
            "FQMA" to Coordinate(-25.920836, 32.572606),
            "FAPE" to Coordinate(-33.984919, 25.617275),
            "SBGR" to Coordinate(-23.432075, -46.469511),
            "SBPA" to Coordinate(-29.994428, -51.171428),
            "SUMU" to Coordinate(-34.838417, -56.030806),
            "SCCI" to Coordinate(-53.002642, -70.854586),
            "SAWH" to Coordinate(-54.843278, -68.29575),
            "YBBN" to Coordinate(-27.37939, 153.12026),
            "YPPH" to Coordinate(-31.940278, 115.966944),
            "NZAA" to Coordinate(-37.008056, 174.791667)
        )
        for ((id, location) in stations) {
            val temperatures = HistoricMonthlyTemperatureRangeRepo.getMonthlyTemperatureRanges(location)
            val precipitation = HistoricMonthlyPrecipitationRepo.getMonthlyPrecipitation(location)
            for ((month, range) in temperatures) {
                val millimeters = precipitation.getValue(month).meters().value * 1000
                Log.i(
                    "ClimateNormals",
                    "$id,${month.value},${range.start.celsius().value},${range.end.celsius().value},$millimeters"
                )
            }
        }
    }
}
