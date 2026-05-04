package com.kylecorry.trail_sense.tools.map.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileGroup

@Entity(tableName = "offline_map_file_groups", indices = [Index(value = ["parent"])])
data class OfflineMapFileGroupEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "parent") val parent: Long? = null
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toOfflineMapFileGroup(): OfflineMapFileGroup {
        return OfflineMapFileGroup(id, name, parent)
    }

    companion object {
        fun from(group: OfflineMapFileGroup): OfflineMapFileGroupEntity {
            return OfflineMapFileGroupEntity(group.name, group.parentId).also {
                it.id = group.id
            }
        }
    }
}
