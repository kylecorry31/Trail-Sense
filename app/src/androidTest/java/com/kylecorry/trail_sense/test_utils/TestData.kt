package com.kylecorry.trail_sense.test_utils

import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.units.PercentCoordinate
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.andromeda.gpx.GPXSerializer
import com.kylecorry.luna.result.getOrNull
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudObservation
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import com.kylecorry.trail_sense.tools.lightning.domain.LightningStrike
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapRequest
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo
import com.kylecorry.sol.science.oceanography.Tide
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

    fun addMapLayer(mapId: String, layerId: String) {
        val repo = MapLayerPreferenceRepo()
        repo.setActiveLayerIds(mapId, repo.getActiveLayerIds(mapId) + layerId)
    }

    // A blank 100x100 image calibrated so the center of the image is at the coordinate and the top
    // left corner is 2 km northwest of it
    fun addPhotoMap(
        name: String,
        center: Coordinate,
        groupId: Long? = null,
        visible: Boolean = true
    ): Long = runBlocking {
        val files = FileSubsystem.getInstance(context)
        val file = files.createTemp(".webp")
        val bitmap = createBitmap(100, 100)
        Canvas(bitmap).drawColor(Color.WHITE)
        files.save(files.getLocalPath(file), bitmap, 10, true)

        val topLeft = center.plus(Distance.kilometers(2f), Bearing.from(CompassDirection.NorthWest))
        val calibration = listOf(
            MapCalibrationPoint(center, PercentCoordinate(0.5f, 0.5f)),
            MapCalibrationPoint(topLeft, PercentCoordinate(0f, 0f)),
        )
        val request = CreateOfflineMapRequest(file.toUri(), name, groupId, calibration, visible)
        getAppService<OfflineMapService>().createMap(request).getOrNull()!!.map.id
    }

    fun addMapGroup(name: String, parentId: Long? = null): Long = runBlocking {
        getAppService<OfflineMapService>().createGroup(name, parentId).id
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
        groupId: Long? = null,
        temporary: Boolean = false
    ): Long = runBlocking {
        val service = PathService.getInstance(context)
        val id = service.addPath(
            Path(0, name, PathStyle.default(), PathMetadata.empty, temporary, groupId)
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

    fun addNote(title: String, contents: String, createdOn: Instant = Instant.now()): Long =
        runBlocking {
            NoteRepo.getInstance(context).addNote(Note(title, contents, createdOn.toEpochMilli()))
        }

    /**
     * Adds a packing list with the given items (their pack ID is replaced with the new pack).
     */
    fun addPack(name: String, items: List<PackItem> = emptyList()): Long = runBlocking {
        val repo = PackRepo.getInstance(context)
        val id = repo.addPack(Pack(0, name))
        items.forEach { repo.addItem(it.copy(packId = id)) }
        id
    }

    fun addLightningStrike(distance: Distance, time: Instant): Long = runBlocking {
        LightningRepo.getInstance(context).add(Reading(LightningStrike(0, distance), time))
    }

    fun addCloud(genus: CloudGenus?, time: Instant): Long = runBlocking {
        CloudRepo.getInstance(context).add(Reading(CloudObservation(0, genus), time))
    }

    fun addTideTable(name: String, location: Coordinate, tides: List<Tide>): Long = runBlocking {
        TideTableRepo.getInstance(context).addTideTable(
            TideTable(0, tides, name, location)
        )
    }

    fun addFieldGuidePage(
        name: String,
        tags: List<FieldGuidePageTag> = emptyList(),
        notes: String? = null
    ): Long = runBlocking {
        FieldGuideRepo.getInstance(context).add(
            FieldGuidePage(0, name, directTags = tags, notes = notes)
        )
    }

    fun addFieldGuideSighting(
        pageId: Long,
        time: Instant? = null,
        location: Coordinate? = null,
        altitudeMeters: Float? = null,
        harvested: Boolean? = null,
        notes: String? = null,
        showOnMap: Boolean = true
    ): Long = runBlocking {
        FieldGuideRepo.getInstance(context).addSighting(
            Sighting(0, pageId, time, location, altitudeMeters, harvested, notes, showOnMap)
        )
    }

    fun builtInFieldGuidePageId(name: String): Long = runBlocking {
        FieldGuideRepo.getInstance(context).getAllPages().first { it.isBuiltIn && it.name == name }.id
    }
}
