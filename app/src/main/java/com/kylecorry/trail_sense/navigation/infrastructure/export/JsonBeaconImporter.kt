package com.kylecorry.trail_sense.navigation.infrastructure.export

import android.content.Context
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.domain.BeaconGroupEntity
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trailsensecore.domain.navigation.BeaconOwner

// TODO: Remove this
class JsonBeaconImporter(private val context: Context) {

    private val repo by lazy { BeaconRepo.getInstance(context) }

    /**
     * Imports beacons into the database from a file
     * @returns the number of beacons imported
     */
    suspend fun import(text: String): Int {
        val dto = JsonConvert.fromJson<BeaconExport>(text) ?: return 0

        val groupIdMap = mutableMapOf<Long, Long>()

        dto.groups.forEach {
            val oldId = it.id
            val newId = repo.addBeaconGroup(BeaconGroupEntity.from(it).also { it.id = 0 })
            groupIdMap[oldId] = newId
        }

        val beaconEntities = dto.beacons.map {
            BeaconEntity.from(it.copy(owner = BeaconOwner.User)).copy(
                beaconGroupId = when {
                    it.beaconGroupId == null -> {
                        null
                    }
                    groupIdMap.containsKey(it.beaconGroupId) -> {
                        groupIdMap[it.beaconGroupId]
                    }
                    else -> {
                        null
                    }
                }
            ).also {
                it.id = 0
            }
        }

        beaconEntities.forEach {
            repo.addBeacon(it)
        }

        return dto.beacons.size
    }

}