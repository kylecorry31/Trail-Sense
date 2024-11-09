package com.kylecorry.trail_sense.settings.backup

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.pickDirectory
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command

class ChangeAutomaticBackupDirectoryCommand(
    private val context: Context,
    private val resolver: IntentResultRetriever,
    private val onChange: (isSuccessful: Boolean) -> Unit = {}
) : Command {
    override fun execute() {
        val prefs = UserPreferences(context)
        resolver.pickDirectory(
            context.getString(R.string.select_backup_directory),
            true,
            true,
            true
        ) {
            if (it == null) {
                Alerts.toast(context, context.getString(R.string.no_directory_selected))
                onChange(false)
                return@pickDirectory
            }
            Intents.acceptPersistentUri(context, it)
            prefs.backup.autoBackupUri = it
            Alerts.dialog(
                context,
                context.getString(R.string.automatic_backup),
                context.getString(
                    R.string.auto_backup_instructions,
                    prefs.backup.userFriendlyAutoBackupPathName,
                    prefs.backup.autoBackupCount
                ),
                cancelText = null
            )
            onChange(true)
        }
    }
}