package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.shared.debugging.isDebug

class FieldGuidePreferences(context: Context) : PreferenceRepo(context) {

    val isSightingsEnabled = isDebug()

}