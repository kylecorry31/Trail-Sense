package com.kylecorry.trail_sense.tools.tides.infrastructure.io

import com.kylecorry.sol.science.oceanography.TidalHarmonic
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoaaHcAndMetadataTideTableConverterTest {
    @Test
    fun parse() {
        val converter = NoaaHcAndMetadataTideTableConverter()
        assertNull(converter.parse("".byteInputStream()))

        val xml =
            """<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<soapenv:Body>
<HCandMetadata xmlns="https://opendap.co-ops.nos.noaa.gov/axis/webservices/harmonicconstituents/wsdl">
<stationId>8452660</stationId>
<stationName>Newport</stationName>
<latitude>41.5043</latitude>
<longitude>-71.3261</longitude>
<state>RI</state>
<dataSource>USDOC/NOAA/NOS/COOPS(Center for Operational Oceanographic Products and Services)</dataSource>
<timeZone>GMT</timeZone>
<unit>Amplitudes are in Meters, Phases are in degrees, referenced to GMT, and Speed in degrees per hour</unit>
<data>
<item>
<constNum>1</constNum>
<name>M2</name>
<amplitude>0.505</amplitude>
<phase>2.3</phase>
<speed>28.984104</speed>
</item>
<item>
<constNum>2</constNum>
<name>S2</name>
<amplitude>0.108</amplitude>
<phase>25.0</phase>
<speed>30.0</speed>
</item>
</data>
</HCandMetadata>
</soapenv:Body>
</soapenv:Envelope>"""

        val table = converter.parse(xml.byteInputStream())
        val expected = TideTable(
            0,
            emptyList(),
            "Newport, RI",
            Coordinate(41.5043, -71.3261),
            isSemidiurnal = true,
            estimator = TideEstimator.Harmonic,
            harmonics = listOf(
                TidalHarmonic(TideConstituent.M2, 0.505f, 2.3f),
                TidalHarmonic(TideConstituent.S2, 0.108f, 25.0f)
            )
        )

        assertEquals(expected, table)
    }
}