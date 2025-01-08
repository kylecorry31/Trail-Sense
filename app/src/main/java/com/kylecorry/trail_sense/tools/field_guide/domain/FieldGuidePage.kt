package com.kylecorry.trail_sense.tools.field_guide.domain

import com.kylecorry.trail_sense.shared.data.Identifiable

data class FieldGuidePage(
    override val id: Long,
    val name: String,
    val images: List<String> = emptyList(),
    val tags: List<FieldGuidePageTag> = emptyList(),
    val notes: String? = null,

    // User specific fields (not exported)
    val sightings: List<Sighting> = emptyList(),
) : Identifiable
