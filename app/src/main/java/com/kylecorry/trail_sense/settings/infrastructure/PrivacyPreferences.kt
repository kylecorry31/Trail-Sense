package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.andromeda.preferences.BooleanPreference

class PrivacyPreferences(context: Context): PreferenceRepo(context) {

    var isScreenshotProtectionOn by BooleanPreference(cache, context.getString(R.string.pref_privacy_screenshot_protection), false)

}