package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage

class FieldGuideRepo private constructor(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).fieldGuidePageDao()
    private val files = FileSubsystem.getInstance(context)

    suspend fun getAllPages(): List<FieldGuidePage> = onIO {
        val saved = dao.getAllPages().map { it.toFieldGuidePage() }
        BuiltInFieldGuide.getFieldGuide(context) + saved
    }

    suspend fun getPage(id: Long): FieldGuidePage? = onIO {
        if (id < 0) {
            BuiltInFieldGuide.getFieldGuidePage(context, id)
        } else {
            dao.getPage(id)?.toFieldGuidePage()
        }
    }

    suspend fun delete(page: FieldGuidePage) = onIO {
        if (page.isReadOnly) {
            return@onIO
        }
        page.images.forEach { files.delete(it) }
        dao.delete(FieldGuidePageEntity.fromFieldGuidePage(page))
    }

    suspend fun add(page: FieldGuidePage): Long = onIO {
        if (page.isReadOnly) {
            return@onIO -1
        }
        // Delete photos if they've changed
        if (page.id != 0L) {
            val existing = dao.getPage(page.id)?.toFieldGuidePage()
            existing?.images?.filter { it !in page.images }?.forEach { files.delete(it) }
        }

        val entity = FieldGuidePageEntity.fromFieldGuidePage(page)
        dao.upsert(entity)
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