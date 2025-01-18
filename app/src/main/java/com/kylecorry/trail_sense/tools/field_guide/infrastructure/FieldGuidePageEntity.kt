package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.luna.text.toLongCompat
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag

@Entity(tableName = "field_guide_pages")
data class FieldGuidePageEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "images") val images: String,
    @ColumnInfo(name = "tags") val tags: String,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "import_id") val importId: Long? = null
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toFieldGuidePage(): FieldGuidePage {
        return FieldGuidePage(
            id,
            name,
            images.split(',').filter { it.isNotBlank() },
            tags.split(',').mapNotNull {
                val id = it.toLongCompat() ?: return@mapNotNull null
                FieldGuidePageTag.entries.withId(id)
            },
            notes,
            isReadOnly = false,
            sightings = emptyList(),
            importId = importId
        )
    }

    companion object {
        fun fromFieldGuidePage(page: FieldGuidePage): FieldGuidePageEntity {
            return FieldGuidePageEntity(
                page.name,
                page.images.joinToString(","),
                page.tags.joinToString(",") { it.id.toString() },
                page.notes,
                page.importId
            ).apply {
                id = page.id
            }
        }
    }
}