package com.termux.app.x11.di

import android.content.Context
import com.termux.app.x11.service.DesktopSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object X11Module {

    @Provides
    @Singleton
    fun provideDesktopSessionManager(
        @ApplicationContext context: Context
    ): DesktopSessionManager {
        return DesktopSessionManager(context)
    }
}
