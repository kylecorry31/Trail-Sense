package com.kylecorry.trail_sense.tools.field_guide.domain

import android.content.Context
import com.kylecorry.trail_sense.shared.events.IEventEmitter
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.field_guide.FieldGuideToolRegistration
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.IFieldGuideRepo
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideTagNameMapper

class FieldGuideService(
    private val context: Context,
    private val repo: IFieldGuideRepo,
    private val eventBus: IEventEmitter
) {

    suspend fun getAllPages(includeHidden: Boolean = false): List<FieldGuidePage> {
        return repo.getAllPages()
            .filter { includeHidden || !it.isHidden }
            .sortedBy { it.name }
    }

    suspend fun deletePage(page: FieldGuidePage) {
        repo.delete(page)
        eventBus.broadcast(FieldGuideToolRegistration.BROADCAST_PAGE_DELETED)
    }

    suspend fun savePage(page: FieldGuidePage): Long {
        val id = repo.add(page)
        val event = if (page.id == 0L) {
            FieldGuideToolRegistration.BROADCAST_PAGE_ADDED
        } else {
            FieldGuideToolRegistration.BROADCAST_PAGE_CHANGED
        }
        eventBus.broadcast(event)
        return id
    }

    suspend fun setPageHidden(pageId: Long, isHidden: Boolean): Boolean {
        if (!repo.isBuiltInPage(pageId)) {
            // Only built-in pages can be hidden right now
            return false
        }
        repo.setPageHidden(pageId, isHidden)
        eventBus.broadcast(FieldGuideToolRegistration.BROADCAST_PAGE_CHANGED)
        return true
    }

    suspend fun recordSighting(sighting: Sighting): Sighting {
        val newId = repo.addSighting(sighting)
        eventBus.broadcast(FieldGuideToolRegistration.BROADCAST_SIGHTING_RECORDED)
        return sighting.copy(id = newId)
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
