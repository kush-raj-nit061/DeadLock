package com.ingray.deadlock.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.ingray.deadlock.data.local.DeadLockDatabase
import com.ingray.deadlock.data.local.dao.AnalyticsEventDao
import com.ingray.deadlock.data.local.dao.FocusSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "deadlock_settings")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DeadLockDatabase =
        Room.databaseBuilder(
            context,
            DeadLockDatabase::class.java,
            DeadLockDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideFocusSessionDao(db: DeadLockDatabase): FocusSessionDao =
        db.focusSessionDao()

    @Provides
    fun provideAnalyticsEventDao(db: DeadLockDatabase): AnalyticsEventDao =
        db.analyticsEventDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
