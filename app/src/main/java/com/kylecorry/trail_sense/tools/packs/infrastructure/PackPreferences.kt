package com.kylecorry.trail_sense.tools.packs.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.StringPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class PackPreferences(context: Context) : PreferenceRepo(context) {

    var packSort by StringPreference(
        cache,
        context.getString(R.string.pref_pack_item_sort),
        "category"
    )

}
