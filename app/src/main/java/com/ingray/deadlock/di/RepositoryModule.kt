package com.ingray.deadlock.di

import com.ingray.deadlock.data.repository.AnalyticsRepositoryImpl
import com.ingray.deadlock.data.repository.NotificationRepositoryImpl
import com.ingray.deadlock.data.repository.ScheduleRepositoryImpl
import com.ingray.deadlock.data.repository.SessionRepositoryImpl
import com.ingray.deadlock.data.repository.SettingsRepositoryImpl
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import com.ingray.deadlock.domain.repository.NotificationRepository
import com.ingray.deadlock.domain.repository.ScheduleRepository
import com.ingray.deadlock.domain.repository.SessionRepository
import com.ingray.deadlock.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
