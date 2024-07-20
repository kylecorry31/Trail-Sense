package com.kylecorry.trail_sense.shared

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object SharedModule {
    @Provides
    @Singleton
    fun provideFormatService(@ApplicationContext context: Context): FormatService {
        return FormatService.getInstance(context)
    }
}