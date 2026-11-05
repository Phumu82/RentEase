package com.rentease.app.di

import com.rentease.app.data.repository.AdminRepositoryImpl
import com.rentease.app.data.repository.AuthRepositoryImpl
import com.rentease.app.data.repository.DocumentUploadRepository
import com.rentease.app.data.repository.DocumentUploadRepositoryImpl
import com.rentease.app.data.repository.OnboardingRepositoryImpl
import com.rentease.app.domain.repository.OnboardingRepository
import com.rentease.app.data.repository.LandlordRepositoryImpl
import com.rentease.app.data.repository.ListingsRepositoryImpl
import com.rentease.app.data.repository.NotificationsRepositoryImpl
import com.rentease.app.data.repository.SavedListingsRepositoryImpl
import com.rentease.app.data.repository.SupportRepositoryImpl
import com.rentease.app.domain.repository.AdminRepository
import com.rentease.app.domain.repository.AuthRepository
import com.rentease.app.domain.repository.LandlordRepository
import com.rentease.app.domain.repository.ListingsRepository
import com.rentease.app.domain.repository.NotificationsRepository
import com.rentease.app.domain.repository.SavedListingsRepository
import com.rentease.app.domain.repository.SupportRepository
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDocumentUploadRepository(impl: DocumentUploadRepositoryImpl): DocumentUploadRepository

    @Binds
    @Singleton
    abstract fun bindListingsRepository(impl: ListingsRepositoryImpl): ListingsRepository

    @Binds
    @Singleton
    abstract fun bindLandlordRepository(impl: LandlordRepositoryImpl): LandlordRepository

    @Binds
    @Singleton
    abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository

    @Binds
    @Singleton
    abstract fun bindSavedListingsRepository(impl: SavedListingsRepositoryImpl): SavedListingsRepository

    @Binds
    @Singleton
    abstract fun bindNotificationsRepository(impl: NotificationsRepositoryImpl): NotificationsRepository

    @Binds
    @Singleton
    abstract fun bindSupportRepository(impl: SupportRepositoryImpl): SupportRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository
}
