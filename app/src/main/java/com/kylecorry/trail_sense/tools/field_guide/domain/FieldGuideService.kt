package com.kylecorry.trail_sense.tools.field_guide.domain

import android.content.Context
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.IFieldGuideRepo
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideTagNameMapper

class FieldGuideService(private val context: Context, private val repo: IFieldGuideRepo) {

    suspend fun getAllPages(): List<FieldGuidePage> {
        return repo.getAllPages().sortedBy { it.name }
    }

    suspend fun deletePage(page: FieldGuidePage) {
        repo.delete(page)
    }

    fun filterPages(pages: List<FieldGuidePage>, filter: String, tagFilter: FieldGuidePageTag?): List<FieldGuidePage> {
        val mapper = FieldGuideTagNameMapper(context)
        return TextUtils.search(filter, pages) { page ->
            listOf(
                page.name,
                page.notes ?: "",
                page.tags.joinToString { mapper.getName(it) })
        }.filter { pages ->
            tagFilter == null || pages.tags.contains(tagFilter)
        }
    }

}
