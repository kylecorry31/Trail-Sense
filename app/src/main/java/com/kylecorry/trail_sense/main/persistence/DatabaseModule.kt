package com.kylecorry.trail_sense.main.persistence

import android.content.Context
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryDao
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconDao
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconGroupDao
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudReadingDao
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningStrikeDao
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapDao
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapGroupDao
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteDao
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackDao
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackItemDao
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathDao
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathGroupDao
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.WaypointDao
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableDao
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.PressureReadingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    fun providePackItemDao(database: AppDatabase): PackItemDao {
        return database.packItemDao()
    }

    @Provides
    fun providePackDao(database: AppDatabase): PackDao {
        return database.packDao()
    }

    @Provides
    fun provideWaypointDao(database: AppDatabase): WaypointDao {
        return database.waypointDao()
    }

    @Provides
    fun provideTideTableDao(database: AppDatabase): TideTableDao {
        return database.tideTableDao()
    }

    @Provides
    fun providePressureDao(database: AppDatabase): PressureReadingDao {
        return database.pressureDao()
    }

    @Provides
    fun provideBeaconDao(database: AppDatabase): BeaconDao {
        return database.beaconDao()
    }

    @Provides
    fun provideBeaconGroupDao(database: AppDatabase): BeaconGroupDao {
        return database.beaconGroupDao()
    }

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    fun provideMapDao(database: AppDatabase): MapDao {
        return database.mapDao()
    }

    @Provides
    fun provideMapGroupDao(database: AppDatabase): MapGroupDao {
        return database.mapGroupDao()
    }

    @Provides
    fun provideBatteryDao(database: AppDatabase): BatteryDao {
        return database.batteryDao()
    }

    @Provides
    fun provideCloudDao(database: AppDatabase): CloudReadingDao {
        return database.cloudDao()
    }

    @Provides
    fun providePathDao(database: AppDatabase): PathDao {
        return database.pathDao()
    }

    @Provides
    fun providePathGroupDao(database: AppDatabase): PathGroupDao {
        return database.pathGroupDao()
    }

    @Provides
    fun provideLightningDao(database: AppDatabase): LightningStrikeDao {
        return database.lightningDao()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return AppDatabase.getInstance(appContext)
    }
}