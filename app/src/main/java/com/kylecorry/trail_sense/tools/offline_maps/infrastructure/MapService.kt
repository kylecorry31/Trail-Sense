package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.trail_sense.shared.grouping.count.GroupCounter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupDeleter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapProjectionType
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.MapRotationCalculator

class MapService private constructor(private val repo: MapRepo) {

    val loader = GroupLoader(this::getGroup, this::getChildren)
    private val counter = GroupCounter(loader)

    private val deleter = object : GroupDeleter<IMap>(loader) {
        override suspend fun deleteItems(items: List<IMap>) {
            // TODO: Bulk delete
            items.filterIsInstance<PhotoMap>().forEach { repo.delete(it) }
            items.filterIsInstance<OfflineMapFile>().forEach { repo.delete(it) }
        }

        override suspend fun deleteGroup(group: IMap) {
            (group as? MapGroup)?.let { repo.delete(it) }
        }
    }

    suspend fun add(map: IMap): Long {
        return when (map) {
            is MapGroup -> repo.add(map)
            is PhotoMap -> repo.add(map)
            is OfflineMapFile -> repo.add(map)
            else -> error("Unexpected map subclass")
        }
    }

    suspend fun delete(map: IMap) {
        deleter.delete(map)
    }

    suspend fun setProjection(map: PhotoMap, projection: MapProjectionType): PhotoMap {
        val newMap = map.copy(metadata = map.metadata.copy(projection = projection))
        val recalculatedRotation = if (newMap.isCalibrated) {
            MapRotationCalculator().calculate(newMap)
        } else {
            newMap.calibration.rotation
        }
        val updatedMap = newMap.copy(
            calibration = newMap.calibration.copy(rotation = recalculatedRotation)
        )
        repo.add(updatedMap)
        return updatedMap
    }

    private suspend fun getGroups(parent: Long?): List<MapGroup> {
        return repo.getMapGroups(parent).map { it.copy(count = counter.count(it.id)) }
    }

    private suspend fun getChildren(parentId: Long?): List<IMap> {
        val maps = repo.getPhotoMaps(parentId) + repo.getVectorMaps(parentId)
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

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: MapService? = null

        @Synchronized
        fun getInstance(context: Context): MapService {
            if (instance == null) {
                instance = MapService(MapRepo.Companion.getInstance(context))
            }
            return instance!!
        }
    }

}
