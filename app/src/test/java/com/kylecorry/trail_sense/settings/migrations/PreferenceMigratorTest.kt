package com.kylecorry.trail_sense.settings.migrations

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.AppState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Duration

class PreferenceMigratorTest {

    private val migrator = PreferenceMigrator.getInstance()
    private val prefs = InMemoryPreferences()

    @AfterEach
    fun tearDown() {
        AppState.isReturningUser = false
    }

    @Test
    fun migratesGpsSmoothingSwitchToPercentage() {
        val migration = PreferenceMigrator.migrations.first { it.fromVersion == 32 }
        for (enabled in listOf(null, false, true)) {
            val settings = InMemoryPreferences()
            enabled?.let { settings.putBoolean("pref_use_filtered_gps", it) }
            migration.action(mock(), settings)
            assertEquals(if (enabled == false) 0 else 50, settings.getInt("pref_gps_smoothing"))
            assertFalse(settings.contains("pref_use_filtered_gps"))
        }
    }

    @Test
    fun migrationPreservesExistingGpsSmoothingPercentage() {
        prefs.putInt("pref_gps_smoothing", 45)
        prefs.putBoolean("pref_use_filtered_gps", false)
        PreferenceMigrator.migrations.first { it.fromVersion == 32 }.action(mock(), prefs)
        assertEquals(45, prefs.getInt("pref_gps_smoothing"))
        assertFalse(prefs.contains("pref_use_filtered_gps"))
    }

    @Test
    fun migratesBacktrackFrequencyForExistingUsers() {
        val settings = InMemoryPreferences()
        val context = backtrackContext()
        settings.putBoolean("backtrack_enabled", false)

        PreferenceMigrator.migrations.first { it.fromVersion == 34 }.action(context, settings)

        assertEquals(Duration.ofMinutes(15), settings.getDuration("backtrack_frequency"))
    }

    @Test
    fun doesNotMigrateBacktrackFrequencyForNewUsers() {
        val settings = InMemoryPreferences()

        PreferenceMigrator.migrations.first { it.fromVersion == 34 }.action(backtrackContext(), settings)

        assertFalse(settings.contains("backtrack_frequency"))
    }

    @Test
    fun migrationPreservesExistingBacktrackFrequency() {
        val settings = InMemoryPreferences()
        settings.putBoolean("backtrack_enabled", true)
        settings.putDuration("backtrack_frequency", Duration.ofMinutes(5))

        PreferenceMigrator.migrations.first { it.fromVersion == 34 }.action(backtrackContext(), settings)

        assertEquals(Duration.ofMinutes(5), settings.getDuration("backtrack_frequency"))
    }

    @Test
    fun runsEveryMigrationInOrderForANewInstall() {
        val ran = migrate()

        assertEquals(PreferenceMigrator.migrations, ran)
        assertVersion(PreferenceMigrator.version)
    }

    @Test
    fun skipsVersionsWithoutAMigration() {
        val ran = migrate()

        // There is no 1 -> 2 migration, so version 2 is reached without running anything
        assertTrue(PreferenceMigrator.migrations.none { it.fromVersion == 1 })
        assertTrue(ran.none { it.fromVersion == 1 })
    }

    @Test
    fun onlyRunsMigrationsAboveTheStoredVersion() {
        prefs.putInt(PreferenceMigrator.VERSION_KEY, PreferenceMigrator.version - 1)

        val ran = migrate()

        assertEquals(
            PreferenceMigrator.migrations.filter { it.fromVersion == PreferenceMigrator.version - 1 },
            ran
        )
        assertVersion(PreferenceMigrator.version)
    }

    @Test
    fun doesNothingWhenAlreadyUpToDate() {
        prefs.putInt(PreferenceMigrator.VERSION_KEY, PreferenceMigrator.version)

        assertEquals(emptyList<PreferenceMigration>(), migrate())
        assertVersion(PreferenceMigrator.version)
    }

    @Test
    fun doesNotDowngradeAVersionFromTheFuture() {
        val future = PreferenceMigrator.version + 5
        prefs.putInt(PreferenceMigrator.VERSION_KEY, future)

        assertEquals(emptyList<PreferenceMigration>(), migrate())
        assertVersion(future)
    }

    @Test
    fun isIdempotent() {
        migrate()

        assertEquals(emptyList<PreferenceMigration>(), migrate())
        assertVersion(PreferenceMigrator.version)
    }

    @Test
    fun retriesAFailedMigrationOnTheNextRun() {
        val failAt = PreferenceMigrator.migrations.first().toVersion
        var shouldFail = true

        assertThrows(IllegalStateException::class.java) {
            migrator.migrate(prefs) { migration ->
                if (shouldFail && migration.toVersion == failAt) {
                    throw IllegalStateException("Migration failed")
                }
            }
        }

        // The version is left below the failed migration so it runs again
        assertVersion(failAt - 1)

        shouldFail = false
        val ran = migrate()

        assertTrue(ran.any { it.toVersion == failAt })
        assertVersion(PreferenceMigrator.version)
    }

    @Test
    fun marksExistingInstallsAsReturningUsers() {
        prefs.putInt(PreferenceMigrator.VERSION_KEY, 1)

        migrate()

        assertTrue(AppState.isReturningUser)
    }

    @Test
    fun doesNotMarkNewInstallsAsReturningUsers() {
        migrate()

        assertFalse(AppState.isReturningUser)
    }

    @Test
    fun everyMigrationAdvancesBySingleVersion() {
        PreferenceMigrator.migrations.forEach {
            assertEquals(
                it.fromVersion + 1,
                it.toVersion,
                "Migration ${it.fromVersion} -> ${it.toVersion} skips a version"
            )
        }
    }

    @Test
    fun everyMigrationIsWithinTheCurrentVersion() {
        PreferenceMigrator.migrations.forEach {
            assertTrue(
                it.fromVersion >= 0 && it.toVersion <= PreferenceMigrator.version,
                "Migration ${it.fromVersion} -> ${it.toVersion} is outside of 0..${PreferenceMigrator.version}"
            )
        }
    }

    @Test
    fun onlyOneMigrationExistsPerVersion() {
        val duplicates = PreferenceMigrator.migrations
            .groupBy { it.fromVersion }
            .filterValues { it.size > 1 }
            .keys

        assertEquals(emptySet<Int>(), duplicates, "Duplicate migrations from version $duplicates")
    }

    private fun migrate(): List<PreferenceMigration> {
        val ran = mutableListOf<PreferenceMigration>()
        migrator.migrate(prefs) { ran.add(it) }
        return ran
    }

    private fun assertVersion(expected: Int) {
        // An unset version means the install has not been migrated yet
        assertEquals(expected, prefs.getInt(PreferenceMigrator.VERSION_KEY) ?: 0)
    }

    private fun backtrackContext(): Context {
        return mock<Context>().also {
            whenever(it.getString(R.string.pref_backtrack_enabled)).thenReturn("backtrack_enabled")
            whenever(it.getString(R.string.pref_backtrack_frequency)).thenReturn("backtrack_frequency")
        }
    }
}
