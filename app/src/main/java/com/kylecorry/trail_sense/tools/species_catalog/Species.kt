package com.kylecorry.trail_sense.tools.species_catalog

import com.kylecorry.trail_sense.shared.data.Identifiable

data class Species(
    override val id: Long,
    val name: String,
    val images: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val notes: String? = null,

    // User specific fields (not exported)
    // TODO: Maybe make separate restriction types (ex. daily bag limit, textual, size, etc.)
    val restrictions: List<String> = emptyList(),
    val sightings: List<Sighting> = emptyList(),
) : Identifiable
