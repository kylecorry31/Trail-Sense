package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

enum class FieldGuideNameDisplay(override val id: Long) : Identifiable {
    Common(1),
    Scientific(2),
    Both(3)
}

class FieldGuidePreferences(context: Context) : PreferenceRepo(context) {

    var nameDisplay by StringEnumPreference(
        cache,
        getString(R.string.pref_field_guide_name_display),
        FieldGuideNameDisplay.entries.associateBy { it.id.toString() },
        FieldGuideNameDisplay.Common
    )
}
