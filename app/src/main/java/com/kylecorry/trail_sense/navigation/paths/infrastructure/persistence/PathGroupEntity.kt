package com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup

@Entity(
    tableName = "path_groups"
)
data class PathGroupEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "parent") val parent: Long? = null
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toPathGroup(): PathGroup {
        return PathGroup(id, name, parent)
    }

    companion object {
        fun from(group: PathGroup): PathGroupEntity {
            return PathGroupEntity(group.name, group.parentId).also {
                it.id = group.id
            }
        }
    }

}