package com.kylecorry.trail_sense.tools.lightning.di

import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.ILightningRepo
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningStrikeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
object LightningStrikeDistanceModule {
    @Provides
    fun provideLightningRepo(repo: LightningRepo): ILightningRepo {
        return repo
    }

    @Provides
    fun provideLightningStrikeDao(database: AppDatabase): LightningStrikeDao {
        return database.lightningDao()
    }
}