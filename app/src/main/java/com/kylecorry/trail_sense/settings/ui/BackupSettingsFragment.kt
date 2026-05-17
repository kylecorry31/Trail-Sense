package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.backup.BackupCommand
import com.kylecorry.trail_sense.settings.backup.ChangeAutomaticBackupDirectoryCommand
import com.kylecorry.trail_sense.settings.backup.RestoreCommand
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import kotlinx.coroutines.launch

class BackupSettingsFragment : AndromedaPreferenceFragment() {

    private val uriPicker by lazy { IntentUriPicker(this, requireContext()) }
    private val backupCommand by lazy { BackupCommand(requireContext(), uriPicker) }
    private val restoreCommand by lazy { RestoreCommand(requireContext(), uriPicker) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.backup_preferences, rootKey)

        onClick(preference(R.string.pref_backup_action)) {
            backup()
        }

        onClick(preference(R.string.pref_restore_action)) {
            restore()
        }

        val autoBackupSwitch = switch(R.string.pref_auto_backup_enabled)
        autoBackupSwitch?.summary = if (prefs.backup.isAutoBackupEnabled) {
            prefs.backup.userFriendlyAutoBackupPathName
        } else {
            null
        }

        onClick(autoBackupSwitch) {
            if (prefs.backup.isAutoBackupEnabled) {
                ChangeAutomaticBackupDirectoryCommand(requireContext(), this) {
                    if (!it) {
                        prefs.backup.isAutoBackupEnabled = false
                        autoBackupSwitch?.summary = null
                    } else {
                        autoBackupSwitch?.summary = prefs.backup.userFriendlyAutoBackupPathName
                    }
                }.execute()
            } else {
                autoBackupSwitch?.summary = null
            }
        }
    }

    private fun backup() {
        lifecycleScope.launch {
            backupCommand.execute()
        }
    }

    private fun restore() {
        lifecycleScope.launch {
            restoreCommand.execute()
        }
    }
}
