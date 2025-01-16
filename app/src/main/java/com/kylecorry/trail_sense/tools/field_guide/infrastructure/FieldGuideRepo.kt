package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage

class FieldGuideRepo private constructor(private val context: Context) {

    suspend fun getAllPages(): List<FieldGuidePage> = onIO {
        BuiltInFieldGuide.getFieldGuide(context)
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