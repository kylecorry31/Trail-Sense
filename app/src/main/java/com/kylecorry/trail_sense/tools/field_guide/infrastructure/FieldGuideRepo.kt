package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage

class FieldGuideRepo private constructor(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).fieldGuidePageDao()

    suspend fun getAllPages(): List<FieldGuidePage> = onIO {
        val saved = dao.getAllPages().map { it.toFieldGuidePage() }
        BuiltInFieldGuide.getFieldGuide(context) + saved
    }

    suspend fun getPage(id: Long): FieldGuidePage? {
        return if (id < 0) {
            BuiltInFieldGuide.getFieldGuidePage(context, id)
        } else {
            dao.getPage(id)?.toFieldGuidePage()
        }
    }

    suspend fun delete(page: FieldGuidePage) {
        if (page.isReadOnly) {
            return
        }
        dao.delete(FieldGuidePageEntity.fromFieldGuidePage(page))
    }

    suspend fun add(page: FieldGuidePage): Long {
        if (page.isReadOnly) {
            return -1
        }
        val entity = FieldGuidePageEntity.fromFieldGuidePage(page)
        return dao.upsert(entity)
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