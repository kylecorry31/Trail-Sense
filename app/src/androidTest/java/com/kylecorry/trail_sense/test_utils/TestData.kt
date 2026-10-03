package com.kylecorry.trail_sense.test_utils

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.andromeda.gpx.GPXSerializer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant

/**
 * Inserts known data directly into the app's storage so tests can assert on exact values instead
 * of whatever the emulator's sensors happen to report.
 */
object TestData {

    /**
     * Removes all seeded and recorded data (paths, beacons, readings, etc.)
     */
    fun clear() {
        AppDatabase.getInstance(context).clearAllTables()
    }

    fun addBeacon(
        name: String,
        coordinate: Coordinate,
        elevation: Float? = null,
        comment: String? = null,
        groupId: Long? = null
    ): Long = runBlocking {
        BeaconService(context).add(
            Beacon(
                0,
                name,
                coordinate,
                comment = comment,
                elevation = elevation,
                parentId = groupId
            )
        )
    }

    fun addBeaconGroup(name: String, parentId: Long? = null): Long = runBlocking {
        BeaconService(context).add(BeaconGroup(0, name, parentId))
    }

    fun addPathGroup(name: String, parentId: Long? = null): Long = runBlocking {
        PathService.getInstance(context).addGroup(PathGroup(0, name, parentId))
    }

    fun addPath(
        name: String,
        points: List<PathPoint>,
        groupId: Long? = null
    ): Long = runBlocking {
        val service = PathService.getInstance(context)
        val id = service.addPath(
            Path(0, name, PathStyle.default(), PathMetadata.empty, parentId = groupId)
        )
        service.addWaypointsToPath(points, id)
        id
    }

    /**
     * Adds a path from a GPX file in the androidTest assets folder (ex. "paths/sprague.gpx").
     */
    fun addPathFromGpx(name: String, assetPath: String, groupId: Long? = null): Long {
        return addPath(name, loadGpxPoints(assetPath), groupId)
    }

    fun loadGpxPoints(assetPath: String): List<PathPoint> {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val gpx = runBlocking {
            AssetFileSystem(testContext).stream(assetPath).use {
                GPXSerializer("Trail Sense").deserialize(it)
            }
        }
        return gpx.tracks[0].segments[0].points.map {
            PathPoint(0, 0, it.coordinate, it.elevation, it.time)
        }
    }

    /**
     * Adds a pressure reading taken [age] ago.
     */
    fun addWeatherReading(
        pressureHpa: Float,
        age: Duration,
        altitudeMeters: Float = 0f,
        temperatureCelsius: Float = 15f,
        humidity: Float? = null
    ) = runBlocking {
        WeatherRepo.getInstance(context).add(
            Reading(
                RawWeatherObservation(
                    0,
                    pressureHpa,
                    altitudeMeters,
                    temperatureCelsius,
                    humidity = humidity
                ),
                Instant.now().minus(age)
            )
        )
    }

    /**
     * Adds evenly spaced readings that move linearly from [startHpa] (at [duration] ago) to
     * [endHpa] (at [endAge] ago).
     */
    fun addPressureTrend(
        startHpa: Float,
        endHpa: Float,
        duration: Duration,
        endAge: Duration = Duration.ofMinutes(1),
        interval: Duration = Duration.ofMinutes(15),
        altitudeMeters: Float = 0f
    ) {
        val steps = (duration.toMinutes() / interval.toMinutes()).toInt()
        for (i in 0..steps) {
            val fraction = i / steps.toFloat()
            addWeatherReading(
                startHpa + (endHpa - startHpa) * fraction,
                endAge + interval.multipliedBy((steps - i).toLong()),
                altitudeMeters
            )
        }
    }
}
