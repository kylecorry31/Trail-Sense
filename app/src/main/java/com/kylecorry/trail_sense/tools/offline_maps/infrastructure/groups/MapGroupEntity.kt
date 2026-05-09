package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup

@Entity(tableName = "map_groups")
data class MapGroupEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "parent") val parent: Long? = null
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toMapGroup(): MapGroup {
        return MapGroup(id, name, parent)
    }

    companion object {
        fun from(group: MapGroup): MapGroupEntity {
            return MapGroupEntity(group.name, group.parentId).also {
                it.id = group.id
            }
        }
    }

}
