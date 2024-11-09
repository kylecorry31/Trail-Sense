package com.kylecorry.trail_sense.settings.backup

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.SettingsToolRegistration
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import java.time.Duration

class BackupToolService(private val context: Context) : ToolService {

    private val prefs = UserPreferences(context)

    override val id: String = SettingsToolRegistration.SERVICE_AUTO_BACKUP

    override val name: String = context.getString(R.string.automatic_backup)

    override fun getFrequency(): Duration {
        return Duration.ofDays(1)
    }

    override fun isRunning(): Boolean {
        return isEnabled() && !isBlocked()
    }

    override fun isEnabled(): Boolean {
        return prefs.backup.isAutoBackupEnabled
    }

    override fun isBlocked(): Boolean {
        return false
    }

    override suspend fun enable() {
        prefs.backup.isAutoBackupEnabled = true
        restart()
    }

    override suspend fun disable() {
        prefs.backup.isAutoBackupEnabled = false
        stop()
    }

    override suspend fun restart() {
        // Always starts - it short circuits if it doesn't need to run
        BackupDailyWorker.start(context)
    }

    override suspend fun stop() {
        BackupDailyWorker.stop(context)
    }
}