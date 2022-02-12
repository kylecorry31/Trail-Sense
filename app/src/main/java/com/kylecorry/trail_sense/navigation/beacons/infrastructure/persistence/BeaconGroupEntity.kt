package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup

@Entity(
    tableName = "beacon_groups"
)
data class BeaconGroupEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "parent") val parent: Long? = null
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toBeaconGroup(): BeaconGroup {
        return BeaconGroup(id, name, parent)
    }

    companion object {
        fun from(group: BeaconGroup): BeaconGroupEntity {
            return BeaconGroupEntity(group.name, group.parentId).also {
                it.id = group.id
            }
        }
    }

}