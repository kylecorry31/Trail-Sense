package com.kylecorry.trail_sense.tools.field_guide.domain

import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.shared.withId

data class FieldGuidePage(
    override val id: Long,
    val name: String = "",
    val images: List<String> = emptyList(),
    val directTags: List<FieldGuidePageTag> = emptyList(),
    val notes: String? = null,
    val isReadOnly: Boolean = false,

    // User specific fields (not exported)
    val sightings: List<Sighting> = emptyList(),
    val importId: Long? = null
) : Identifiable {
    val tags: List<FieldGuidePageTag> by lazy {
        val tagQueue = directTags.toMutableList()
        val tags = mutableListOf<FieldGuidePageTag>()
        while (tagQueue.isNotEmpty()) {
            val tag = tagQueue.removeAt(0)
            if (tags.any { it.id == tag.id }) {
                continue
            }
            tags.add(tag)
            val parent = tag.parentId?.let { FieldGuidePageTag.entries.withId(it) }
            if (parent != null) {
                tagQueue.add(parent)
            }
        }

        if (tags.none { it.type == FieldGuidePageTagType.Classification }) {
            tags.add(FieldGuidePageTag.Other)
        }

        tags
    }
}
