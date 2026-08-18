package com.kylecorry.trail_sense.settings.migrations

import com.kylecorry.trail_sense.main.AppState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PreferenceMigratorTest {

    private val migrator = PreferenceMigrator.getInstance()
    private val prefs = InMemoryPreferences()

    @AfterEach
    fun tearDown() {
        AppState.isReturningUser = false
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
}
