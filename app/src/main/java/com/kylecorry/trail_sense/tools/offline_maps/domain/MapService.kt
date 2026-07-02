package com.kylecorry.trail_sense.tools.offline_maps.domain

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.trail_sense.shared.grouping.count.GroupCounter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupDeleter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections.MapProjectionType
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.MapRotationCalculator

class MapService private constructor(private val repo: MapRepo) {

    val loader = GroupLoader(this::getGroup, this::getChildren)
    private val counter = GroupCounter(loader)

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

    suspend fun add(map: OfflineMapCatalogItem): Long {
        return when (map) {
            is MapGroup -> repo.add(map)
            is PhotoMap -> repo.add(map)
            is TrailMap -> repo.add(map)
            else -> error("Unexpected map subclass")
        }
    }

    suspend fun delete(map: OfflineMapCatalogItem) {
        deleter.delete(map)
    }

    suspend fun setProjection(map: PhotoMap, projection: MapProjectionType): PhotoMap {
        val newMap = map.copy(georeference = map.georeference.copy(projectionType = projection))
        val recalculatedRotation = if (newMap.state == OfflineMapState.Ready) {
            MapRotationCalculator().calculate(newMap)
        } else {
            newMap.georeference.rotation
        }
        val updatedMap = newMap.copy(
            georeference = newMap.georeference.copy(rotation = recalculatedRotation)
        )
        repo.add(updatedMap)
        return updatedMap
    }

    private suspend fun getGroups(parent: Long?): List<MapGroup> {
        return repo.getMapGroups(parent).map { it.copy(count = counter.count(it.id)) }
    }

    private suspend fun getChildren(parentId: Long?): List<OfflineMapCatalogItem> {
        val maps = repo.getPhotoMaps(parentId) + repo.getTrailMaps(parentId)
        val groups = getGroups(parentId)
        return maps + groups
    }

    suspend fun getGroup(id: Long?): MapGroup? {
        id ?: return null
        return repo.getMapGroup(id)?.copy(count = counter.count(id))
    }

    suspend fun getAllPhotoMaps(): List<PhotoMap> {
        return repo.getPhotoMaps()
    }

    suspend fun getAllTrailMaps(): List<TrailMap> {
        return repo.getTrailMaps()
    }

    suspend fun getTrailMap(id: Long): TrailMap? {
        return repo.getTrailMap(id)
    }

    suspend fun copyToAppStorage(map: TrailMap): TrailMap? {
        return repo.copyToAppStorage(map)
    }

    suspend fun getPhotoMap(id: Long): PhotoMap? {
        return repo.getPhotoMap(id)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: MapService? = null

        @Synchronized
        fun getInstance(context: Context): MapService {
            if (instance == null) {
                instance = MapService(MapRepo.getInstance(context))
            }
            return instance!!
        }
    }

}
