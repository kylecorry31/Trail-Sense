package com.kylecorry.trail_sense.tools.tides.infrastructure.io

import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.xml.XMLConvert
import com.kylecorry.sol.science.oceanography.TidalHarmonic
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator
import java.io.InputStream

/**
 * A converter for NOAA HC and metadata tide tables (https://opendap.co-ops.nos.noaa.gov/axis/webservices/harmonicconstituents/index.jsp)
 */
class NoaaHcAndMetadataTideTableConverter: TideTableParser {

    override fun parse(stream: InputStream): TideTable? {
        return tryOrDefault(null) {
            val converted =
                XMLConvert.parse(stream, false).children.getOrNull(0)?.children?.getOrNull(0)
                    ?: return null

            val stationName =
                converted.children.find { it.tag == "stationName" }?.text
            val state = converted.children.find { it.tag == "state" }?.text
            val latitude = converted.children.find { it.tag == "latitude" }?.text?.toDoubleOrNull()
            val longitude =
                converted.children.find { it.tag == "longitude" }?.text?.toDoubleOrNull()
            val timezone = converted.children.find { it.tag == "timeZone" }?.text

            if (timezone != "GMT") {
                return null
            }

            val units = converted.children.find { it.tag == "unit" }?.text
            val amplitudeToMeters =
                if (units?.lowercase()?.contains("feet") == true) DistanceUnits.Feet.meters else 1f

            val data = converted.children.find { it.tag == "data" } ?: return null

            val harmonics = data.children.mapNotNull {
                if (it.tag != "item") {
                    return@mapNotNull null
                }

                val constNum = it.children.find { it.tag == "constNum" }?.text?.toLongOrNull()
                    ?: return@mapNotNull null
                val amplitude = it.children.find { it.tag == "amplitude" }?.text?.toFloatOrNull()
                    ?: return@mapNotNull null
                val phase = it.children.find { it.tag == "phase" }?.text?.toFloatOrNull()
                    ?: return@mapNotNull null
                TidalHarmonic(TideConstituent.entries.find { it.id == constNum }
                    ?: return@mapNotNull null, amplitude * amplitudeToMeters, phase)
            }

            val names = listOfNotNull(stationName, state)

            return TideTable(
                0,
                emptyList(),
                if (names.isEmpty()) null else names.joinToString(", "),
                if (latitude != null && longitude != null) Coordinate(
                    latitude,
                    longitude
                ) else null,
                isSemidiurnal = harmonics.maxByOrNull { it.amplitude }?.constituent?.toString()
                    ?.contains("2") == true,
                estimator = TideEstimator.Harmonic,
                harmonics = harmonics,
            )
        }
    }

}