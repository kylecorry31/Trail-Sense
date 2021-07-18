package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.persistence.preferences.StringPreference

class PackPreferences(context: Context): PreferenceRepo(context) {

    var packSort by StringPreference(cache, context.getString(R.string.pref_pack_item_sort), "category")

}