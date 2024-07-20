package com.kylecorry.trail_sense.tools.lightning.di

import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.ILightningRepo
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningStrikeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object LightningStrikeDistanceModule {

    @Singleton
    @Provides
    fun provideLightningRepo(dao: LightningStrikeDao): ILightningRepo {
        return LightningRepo(dao)
    }
}