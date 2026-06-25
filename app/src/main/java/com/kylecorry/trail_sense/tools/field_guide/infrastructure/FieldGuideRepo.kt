package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.getUpsertedId
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting

class FieldGuideRepo private constructor(private val context: Context) : IFieldGuideRepo {

    private val pageDao = AppDatabase.getInstance(context).fieldGuidePageDao()
    private val sightingDao = AppDatabase.getInstance(context).fieldGuideSightingDao()
    private val files = FileSubsystem.getInstance(context)

    override suspend fun getAllPages(): List<FieldGuidePage> = onIO {
        val saved = pageDao.getAllPages().map { it.toFieldGuidePage() }
        val sightings = sightingDao.getAllSightings().map { it.toSighting() }
        val all = BuiltInFieldGuide.getFieldGuide(context) + saved
        all.map { it.copy(sightings = sightings.filter { s -> s.fieldGuidePageId == it.id }) }
    }

    override suspend fun getPage(id: Long): FieldGuidePage? = onIO {
        val page = if (id < 0) {
            BuiltInFieldGuide.getFieldGuidePage(context, id)
        } else {
            pageDao.getPage(id)?.toFieldGuidePage()
        }

        val sightings = sightingDao.getSightingsForPage(id).map { it.toSighting() }
        page?.copy(sightings = sightings)
    }

    override suspend fun delete(page: FieldGuidePage) = onIO {
        if (page.isReadOnly) {
            return@onIO
        }
        page.images.forEach { files.delete(it) }
        sightingDao.deleteAllSightingsForPage(page.id)
        pageDao.delete(FieldGuidePageEntity.fromFieldGuidePage(page))
    }

    override suspend fun add(page: FieldGuidePage): Long = onIO {
        if (page.isReadOnly) {
            return@onIO -1
        }
        // Delete photos if they've changed
        if (page.id != 0L) {
            val existing = pageDao.getPage(page.id)?.toFieldGuidePage()
            existing?.images?.filter { it !in page.images }?.forEach { files.delete(it) }
        }

        val entity = FieldGuidePageEntity.fromFieldGuidePage(page)
        pageDao.upsert(entity).getUpsertedId(page.id)
    }

    override suspend fun addSighting(sighting: Sighting): Long = onIO {
        sightingDao.upsert(FieldGuideSightingEntity.fromSighting(sighting)).getUpsertedId(sighting.id)
    }

    override suspend fun getSighting(id: Long): Sighting? = onIO {
        sightingDao.getSighting(id)?.toSighting()
    }

    override suspend fun deleteSighting(sighting: Sighting) = onIO {
        sightingDao.delete(FieldGuideSightingEntity.fromSighting(sighting))
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: FieldGuideRepo? = null

        @Synchronized
        fun getInstance(context: Context): FieldGuideRepo {
            if (instance == null) {
                instance = FieldGuideRepo(context)
            }
            return instance!!
        }
    }
}
