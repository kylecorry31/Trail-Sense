package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R

class PrivacyPreferences(context: Context): PreferenceRepo(context) {

    var isScreenshotProtectionOn by BooleanPreference(cache, context.getString(R.string.pref_privacy_screenshot_protection), false)

}