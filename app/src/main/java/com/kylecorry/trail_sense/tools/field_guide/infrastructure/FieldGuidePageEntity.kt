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
    @ColumnInfo(name = "scientific_name") val scientificName: String? = null,
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
            id = id,
            name = name,
            scientificName = scientificName,
            images = images.split(',').filter { it.isNotBlank() },
            directTags = tags.split(',').mapNotNull {
                val id = it.toLongCompat() ?: return@mapNotNull null
                FieldGuidePageTag.entries.withId(id)
            },
            notes = notes,
            isReadOnly = false,
            sightings = emptyList(),
            importId = importId
        )
    }

    companion object {
        fun fromFieldGuidePage(page: FieldGuidePage): FieldGuidePageEntity {
            return FieldGuidePageEntity(
                name = page.name,
                scientificName = page.scientificName,
                images = page.images.joinToString(","),
                tags = page.tags.joinToString(",") { it.id.toString() },
                notes = page.notes,
                importId = page.importId
            ).apply {
                id = page.id
            }
        }
    }
}
