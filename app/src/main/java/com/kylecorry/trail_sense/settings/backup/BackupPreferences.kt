package com.kylecorry.trail_sense.settings.backup

import android.content.Context
import android.net.Uri
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.StringPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class BackupPreferences(context: Context) : PreferenceRepo(context) {

    var isAutoBackupEnabled by BooleanPreference(
        cache,
        getString(R.string.pref_auto_backup_enabled),
        false
    )

    private var autoBackupUriString: String by StringPreference(
        cache,
        getString(R.string.pref_auto_backup_uri),
        ""
    )

    var autoBackupUri: Uri?
        get() {
            return if (autoBackupUriString.isBlank()) {
                null
            } else {
                Uri.parse(autoBackupUriString)
            }
        }
        set(value) {
            autoBackupUriString = value?.toString() ?: ""
        }

    val userFriendlyAutoBackupPathName: String
        get() {
            val path = autoBackupUri?.path ?: return ""
            val colonIndex = path.indexOf(":")
            return if (colonIndex != -1) {
                path.substring(colonIndex + 1)
            } else {
                path
            }
        }

    val autoBackupCount = 2

}