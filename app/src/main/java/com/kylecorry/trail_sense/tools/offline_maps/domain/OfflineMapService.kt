package com.kylecorry.trail_sense.tools.offline_maps.domain

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.kylecorry.andromeda.bitmaps.BitmapUtils.fixPerspective
import com.kylecorry.andromeda.core.units.PercentBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.grouping.count.GroupCounter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupDeleter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupLoader
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapResolution
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections.MapProjectionType
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.selection.ActiveMapSelector
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.OfflineMapImporter
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.OfflineMapMaintenance
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.MapRotationCalculator
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce.HighQualityMapReducer
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce.LowQualityMapReducer
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce.MediumQualityMapReducer
import java.io.IOException

class OfflineMapService internal constructor(
    context: Context,
    private val repo: MapRepo,
    private val files: FileSubsystem,
    private val prefs: UserPreferences
) {
    val loader = GroupLoader(this::getGroup, this::getChildren)

    private val maintenance = OfflineMapMaintenance(files, repo)
    private val importer = OfflineMapImporter(context, files, prefs)
    private val counter = GroupCounter(loader)
    private val rotationCalculator = MapRotationCalculator()
    private val activeMapSelector = ActiveMapSelector()

    private val deleter = object : GroupDeleter<OfflineMapCatalogItem>(loader) {
        override suspend fun deleteItems(items: List<OfflineMapCatalogItem>) {
            // TODO: Bulk delete
            items.filterIsInstance<PhotoMap>().forEach { repo.delete(it) }
            items.filterIsInstance<TrailMap>().forEach { repo.delete(it) }
        }

        override suspend fun deleteGroup(group: OfflineMapCatalogItem) {
            (group as? MapGroup)?.let { repo.delete(it) }
        }
    }

    suspend fun createGroup(name: String, parentId: Long? = null): MapGroup {
        return add(MapGroup(0, name, parentId))
    }

    suspend fun createMap(request: CreateOfflineMapRequest): CreateOfflineMapResult? {
        return maintenance.withImportLock {
            val imported = importer.import(request)?.let {
                add(it)
            } ?: return@withImportLock null
            val reduced = maybeReduce(imported)
            CreateOfflineMapResult(
                reduced,
                reduced is PhotoMap && reduced.georeference.calibrationPoints.isNotEmpty()
            )
        }
    }

    suspend fun rename(map: OfflineMapCatalogItem, name: String): OfflineMapCatalogItem {
        val updated = when (map) {
            is MapGroup -> map.copy(name = name)
            is PhotoMap -> map.copy(name = name)
            is TrailMap -> map.copy(name = name)
            else -> error("Unexpected map subclass")
        }
        return add(updated)
    }

    suspend fun move(map: OfflineMapCatalogItem, parentId: Long?): OfflineMapCatalogItem {
        val updated = when (map) {
            is MapGroup -> map.copy(parentId = parentId)
            is PhotoMap -> map.copy(parentId = parentId)
            is TrailMap -> map.copy(parentId = parentId)
            else -> error("Unexpected map subclass")
        }
        return add(updated)
    }

    suspend fun setAttribution(map: TrailMap, attribution: String?): TrailMap {
        return add(map.copy(attribution = attribution?.trim()))
    }

    suspend fun setVisible(map: OfflineMapCatalogItem, visible: Boolean) {
        when (map) {
            is MapGroup -> {
                loader.getChildren(map.id, null).forEach {
                    setVisible(it, visible)
                }
            }

            is PhotoMap -> {
                if (map.visible != visible) {
                    add(map.copy(visible = visible))
                }
            }

            is TrailMap -> {
                if (map.visible != visible) {
                    add(map.copy(visible = visible))
                }
            }

            else -> error("Unexpected map subclass")
        }
    }

    suspend fun cleanup(): Boolean {
        return maintenance.cleanup()
    }

    suspend fun reduce(map: PhotoMap, resolution: PhotoMapResolution): PhotoMap {
        val reducer = when (resolution) {
            PhotoMapResolution.Low -> LowQualityMapReducer(files)
            PhotoMapResolution.Medium -> MediumQualityMapReducer(files)
            PhotoMapResolution.High -> HighQualityMapReducer(files)
        }
        return add(reducer.reduce(map))
    }

    suspend fun warp(map: PhotoMap, bounds: PercentBounds?): PhotoMap? {
        if (bounds != null) {
            val bitmap = files.bitmap(map.imageFile.path) ?: return null
            val pixelBounds = bounds.toPixelBounds(bitmap.width.toFloat(), bitmap.height.toFloat())
            val warped = bitmap.fixPerspective(pixelBounds, true, Color.WHITE)
            try {
                files.save(map.imageFile.path, warped, recycleOnSave = true)
            } catch (e: IOException) {
                Log.e("MapService", "Failed to save warped map", e)
                return null
            }

            map.pdfFile?.let { files.delete(it.path) }
        }

        val updated = map.copy(
            georeference = map.georeference.copy(isWarpingCompleted = true)
        )
        return add(updated)
    }

    suspend fun calibrate(map: PhotoMap, points: List<MapCalibrationPoint>): PhotoMap {
        val updated = map.copy(
            georeference = map.georeference.copy(calibrationPoints = points)
        )
        return add(updated)
    }

    suspend fun autoRotate(map: PhotoMap): PhotoMap {
        if (map.state != OfflineMapState.Ready) {
            return map
        }

        val updated = map.copy(
            georeference = map.georeference.copy(
                rotation = rotationCalculator.calculate(map)
            )
        )
        return add(updated)
    }

    suspend fun delete(map: OfflineMapCatalogItem) {
        deleter.delete(map)
    }

    suspend fun setProjection(map: PhotoMap, projection: MapProjectionType): PhotoMap {
        val newMap = map.copy(georeference = map.georeference.copy(projectionType = projection))
        val recalculatedRotation = if (newMap.state == OfflineMapState.Ready) {
            rotationCalculator.calculate(newMap)
        } else {
            newMap.georeference.rotation
        }
        val updatedMap = newMap.copy(
            georeference = newMap.georeference.copy(rotation = recalculatedRotation)
        )
        return add(updatedMap)
    }

    suspend fun getGroup(id: Long?): MapGroup? {
        id ?: return null
        return repo.getMapGroup(id)?.copy(count = counter.count(id))
    }

    suspend fun getActivePhotoMap(location: Coordinate, destination: Coordinate?): PhotoMap? {
        return activeMapSelector.getActiveMap(repo.getPhotoMaps(), location, destination)
    }

    suspend fun getVisibleTrailMapAttributions(): List<String> {
        return repo.getTrailMaps()
            .filter { it.visible }
            .mapNotNull { it.attribution?.trim()?.takeIf { attribution -> attribution.isNotBlank() } }
            .distinct()
    }

    suspend fun getRenderablePhotoMaps(featureId: Long?): List<PhotoMap> {
        return (if (featureId == null) {
            repo.getPhotoMaps().filter { it.visible }
        } else {
            listOfNotNull(getPhotoMap(featureId))
        }).filter { it.state == OfflineMapState.Ready }
    }

    suspend fun getRenderableTrailMaps(featureId: Long?): List<TrailMap> {
        return (if (featureId == null) {
            repo.getTrailMaps().filter { it.visible }
        } else {
            listOfNotNull(getTrailMap(featureId))
        }).filter { it.state == OfflineMapState.Ready }
    }

    suspend fun getTrailMap(id: Long): TrailMap? {
        return repo.getTrailMap(id)
    }

    suspend fun copyToAppStorage(map: TrailMap): TrailMap? {
        return maintenance.withImportLock {
            repo.copyToAppStorage(map)
        }
    }

    suspend fun getPhotoMap(id: Long): PhotoMap? {
        return repo.getPhotoMap(id)
    }

    private suspend fun getGroups(parent: Long?): List<MapGroup> {
        return repo.getMapGroups(parent).map { it.copy(count = counter.count(it.id)) }
    }

    private suspend fun getChildren(parentId: Long?): List<OfflineMapCatalogItem> {
        val maps = repo.getPhotoMaps(parentId) + repo.getTrailMaps(parentId)
        val groups = getGroups(parentId)
        return maps + groups
    }

    private suspend fun maybeReduce(map: OfflineMap): OfflineMap {
        if (map !is PhotoMap) {
            return map
        }

        val isPdfMap = map.pdfFile != null
        val shouldReduce = (isPdfMap && prefs.photoMaps.autoReducePdfMaps) ||
                (!isPdfMap && prefs.photoMaps.autoReducePhotoMaps)
        if (!shouldReduce) {
            return map
        }

        return reduce(map, PhotoMapResolution.High)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : OfflineMapCatalogItem> add(map: T): T {
        return when (map) {
            is MapGroup -> {
                val id = repo.add(map)
                map.copy(id = id)
            }

            is PhotoMap -> {
                val id = repo.add(map)
                map.copy(id = id)
            }

            is TrailMap -> {
                val id = repo.add(map)
                map.copy(id = id)
            }

            else -> error("Unexpected map subclass")
        } as T
    }
}
