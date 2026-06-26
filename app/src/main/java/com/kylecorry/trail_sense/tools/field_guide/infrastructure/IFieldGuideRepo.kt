package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting

interface IFieldGuideRepo {
    suspend fun getAllPages(): List<FieldGuidePage>

    suspend fun getPage(id: Long): FieldGuidePage?

    suspend fun delete(page: FieldGuidePage)

    suspend fun add(page: FieldGuidePage): Long

    suspend fun addSighting(sighting: Sighting): Long

    suspend fun getSighting(id: Long): Sighting?

    suspend fun deleteSighting(sighting: Sighting)
}
