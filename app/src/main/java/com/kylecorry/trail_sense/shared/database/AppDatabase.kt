package com.kylecorry.trail_sense.shared.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconDao
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconEntity
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconGroupDao
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconGroupEntity
import com.kylecorry.trail_sense.navigation.paths.domain.WaypointEntity
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.*
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReadingEntity
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryDao
import com.kylecorry.trail_sense.tools.maps.domain.MapEntity
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapDao
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteDao
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackDao
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackEntity
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackItemDao
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackItemEntity
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableDao
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableDatabaseMigrationWorker
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableEntity
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRowEntity
import com.kylecorry.trail_sense.weather.infrastructure.persistence.CloudReadingDao
import com.kylecorry.trail_sense.weather.infrastructure.persistence.CloudReadingEntity
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureReadingDao
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureReadingEntity

/**
 * The Room database for this app
 */
@Database(
    entities = [PackItemEntity::class, Note::class, WaypointEntity::class, PressureReadingEntity::class, BeaconEntity::class, BeaconGroupEntity::class, MapEntity::class, BatteryReadingEntity::class, PackEntity::class, CloudReadingEntity::class, PathEntity::class, TideTableEntity::class, TideTableRowEntity::class, PathGroupEntity::class],
    version = 27,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packItemDao(): PackItemDao
    abstract fun packDao(): PackDao
    abstract fun waypointDao(): WaypointDao
    abstract fun tideTableDao(): TideTableDao
    abstract fun pressureDao(): PressureReadingDao
    abstract fun beaconDao(): BeaconDao
    abstract fun beaconGroupDao(): BeaconGroupDao
    abstract fun noteDao(): NoteDao
    abstract fun mapDao(): MapDao
    abstract fun batteryDao(): BatteryDao
    abstract fun cloudDao(): CloudReadingDao
    abstract fun pathDao(): PathDao
    abstract fun pathGroupDao(): PathGroupDao

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {

            val MIGRATION_1_2 = object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `contents` TEXT, `created` INTEGER NOT NULL)")
                }
            }

            val MIGRATION_2_3 = object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `waypoints` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `altitude` REAL, `createdOn` INTEGER NOT NULL)")
                }
            }

            val MIGRATION_3_4 = object : Migration(3, 4) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `pressures` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pressure` REAL NOT NULL, `altitude` REAL NOT NULL, `altitude_accuracy` REAL, `temperature` REAL NOT NULL, `time` INTEGER NOT NULL)")
                }
            }

            val MIGRATION_4_5 = object : Migration(4, 5) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `beacons` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `visible` INTEGER NOT NULL DEFAULT 1, `comment` TEXT DEFAULT NULL, `beacon_group_id` INTEGER DEFAULT NULL, `elevation` REAL DEFAULT NULL)")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `beacon_groups` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                }
            }

            val MIGRATION_5_6 = object : Migration(5, 6) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `beacons` ADD COLUMN `temporary` INTEGER NOT NULL DEFAULT 0")
                }
            }

            val MIGRATION_6_7 = object : Migration(6, 7) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `waypoints` ADD COLUMN `cellType` INTEGER DEFAULT NULL")
                    database.execSQL("ALTER TABLE `waypoints` ADD COLUMN `cellQuality` INTEGER DEFAULT NULL")
                }
            }

            val MIGRATION_7_8 = object : Migration(7, 8) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `tides` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `reference_high` INTEGER NOT NULL, `name` TEXT DEFAULT NULL, `latitude` REAL DEFAULT NULL, `longitude` REAL DEFAULT NULL)")
                }
            }

            val MIGRATION_8_9 = object : Migration(8, 9) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `pressures` ADD COLUMN `humidity` REAL NOT NULL DEFAULT 0")
                }
            }

            val MIGRATION_9_10 = object : Migration(9, 10) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `maps` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `filename` TEXT NOT NULL, `latitude1` REAL DEFAULT NULL, `longitude1` REAL DEFAULT NULL, `percentX1` REAL DEFAULT NULL, `percentY1` REAL DEFAULT NULL, `latitude2` REAL DEFAULT NULL, `longitude2` REAL DEFAULT NULL, `percentX2` REAL DEFAULT NULL, `percentY2` REAL DEFAULT NULL)")
                }
            }

            val MIGRATION_10_11 = object : Migration(10, 11) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `battery` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `percent` REAL NOT NULL, `isCharging` INTEGER NOT NULL, `time` INTEGER NOT NULL)")
                }
            }

            val MIGRATION_11_12 = object : Migration(11, 12) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `battery` ADD COLUMN `capacity` REAL NOT NULL DEFAULT 0")
                }
            }

            val MIGRATION_12_13 = object : Migration(12, 13) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `beacons` ADD COLUMN `color` INTEGER NOT NULL DEFAULT 1")
                    database.execSQL("ALTER TABLE `beacons` ADD COLUMN `owner` INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("UPDATE `beacons` SET `owner` = 1 WHERE `temporary` = 1")
                }
            }

            val MIGRATION_13_14 = object : Migration(13, 14) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `maps` ADD COLUMN `warped` INTEGER NOT NULL DEFAULT 1")
                    database.execSQL("ALTER TABLE `maps` ADD COLUMN `rotated` INTEGER NOT NULL DEFAULT 1")
                }
            }

            val MIGRATION_14_15 = object : Migration(14, 15) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `items` ADD COLUMN `desiredAmount` REAL NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE `items` ADD COLUMN `weight` REAL DEFAULT NULL")
                    database.execSQL("ALTER TABLE `items` ADD COLUMN `weightUnits` INTEGER DEFAULT NULL")
                    database.execSQL("ALTER TABLE `items` ADD COLUMN `packId` INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `packs` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                }
            }

            val MIGRATION_15_16 = object : Migration(15, 16) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `waypoints` ADD COLUMN `pathId` INTEGER NOT NULL DEFAULT 0")
                }
            }

            val MIGRATION_16_17 = object : Migration(16, 17) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `clouds` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `time` INTEGER NOT NULL, `cover` REAL NOT NULL)")
                }
            }

            val MIGRATION_17_18 = object : Migration(17, 18) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `paths` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `lineStyle` INTEGER NOT NULL, `pointStyle` INTEGER NOT NULL, `color` INTEGER NOT NULL, `visible` INTEGER NOT NULL, `temporary` INTEGER NOT NULL, `distance` REAL NOT NULL, `numWaypoints` INTEGER NOT NULL, `startTime` INTEGER, `endTime` INTEGER, `north` REAL NOT NULL, `east` REAL NOT NULL, `south` REAL NOT NULL, `west` REAL NOT NULL)")
                    val request =
                        OneTimeWorkRequestBuilder<PathDatabaseMigrationWorker>().build()
                    WorkManager.getInstance(context).enqueue(request)
                }
            }

            val MIGRATION_18_19 = object : Migration(18, 19) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `maps` ADD COLUMN `projection` INTEGER NOT NULL DEFAULT 1")
                    database.execSQL("ALTER TABLE `maps` ADD COLUMN `rotation` INTEGER NOT NULL DEFAULT 0")
                }
            }

            val MIGRATION_19_20 = object : Migration(19, 20) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `tides` ADD COLUMN `mtl` REAL DEFAULT NULL")
                    database.execSQL("ALTER TABLE `tides` ADD COLUMN `mllw` REAL DEFAULT NULL")
                    database.execSQL("ALTER TABLE `tides` ADD COLUMN `mn` REAL DEFAULT NULL")
                }
            }

            val MIGRATION_20_21 = object : Migration(20, 21) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `tides` ADD COLUMN `diurnal` INTEGER NOT NULL DEFAULT 0")
                }
            }

            val MIGRATION_21_22 = object : Migration(21, 22) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `tide_tables` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `latitude` REAL, `longitude` REAL)")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `tide_table_rows` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `table_id` INTEGER NOT NULL, `time` INTEGER NOT NULL, `high` INTEGER NOT NULL, `height` REAL)")
                    val request =
                        OneTimeWorkRequestBuilder<TideTableDatabaseMigrationWorker>().build()
                    WorkManager.getInstance(context).enqueue(request)
                }
            }

            val MIGRATION_22_23 = object : Migration(22, 23) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `beacon_groups` ADD COLUMN `parent` INTEGER DEFAULT NULL")
                }
            }

            val MIGRATION_23_24 = object : Migration(23, 24) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `path_groups` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `parent` INTEGER DEFAULT NULL)")
                    database.execSQL("ALTER TABLE `paths` ADD COLUMN `parentId` INTEGER DEFAULT NULL")
                }
            }

            val MIGRATION_24_25 = object : Migration(24, 25) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("DROP TABLE tides")
                    database.execSQL("ALTER TABLE `tide_tables` ADD COLUMN `isSemidiurnal` INTEGER NOT NULL DEFAULT 1")
                    database.execSQL("ALTER TABLE `tide_tables` ADD COLUMN `isVisible` INTEGER NOT NULL DEFAULT 1")
                }
            }

            val MIGRATION_25_26 = object : Migration(25, 26) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `beacons` ADD COLUMN `icon` INTEGER DEFAULT NULL")
                }
            }

            val MIGRATION_26_27 = object : Migration(26, 27) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `clouds` ADD COLUMN `genus` INTEGER NOT NULL DEFAULT 1")
                }
            }

            return Room.databaseBuilder(context, AppDatabase::class.java, "trail_sense")
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_20_21,
                    MIGRATION_21_22,
                    MIGRATION_22_23,
                    MIGRATION_23_24,
                    MIGRATION_24_25,
                    MIGRATION_25_26,
                    MIGRATION_26_27
                )
                .build()
        }
    }
}