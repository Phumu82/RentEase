package com.rentease.app.di

import com.rentease.app.data.remote.SupabaseEndpoint
import com.rentease.app.BuildConfig
import com.rentease.app.data.auth.AuthInterceptor
import com.rentease.app.data.remote.RetrofitFactory
import com.rentease.app.data.remote.api.SupabaseAdminAccessApi
import com.rentease.app.data.remote.api.SupabaseAdminApi
import com.rentease.app.data.remote.api.SupabaseAuthApi
import com.rentease.app.data.remote.api.SupabaseLandlordApi
import com.rentease.app.data.remote.api.SupabaseListingsApi
import com.rentease.app.data.remote.api.SupabaseNotificationsApi
import com.rentease.app.data.remote.api.SupabaseRestApi
import com.rentease.app.data.remote.api.SupabaseStorageApi
import com.rentease.app.data.remote.api.SupabaseSupportApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            redactHeader("apikey")
            // Never BODY, even in debug: sign-in and sign-up bodies carry the password and the tokens.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC // method, URL and status code - enough to debug 4xx/5xx
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        RetrofitFactory.create(client, SupabaseEndpoint.baseUrl)

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): SupabaseAuthApi = retrofit.create(SupabaseAuthApi::class.java)

    @Provides
    @Singleton
    fun provideRestApi(retrofit: Retrofit): SupabaseRestApi = retrofit.create(SupabaseRestApi::class.java)

    @Provides
    @Singleton
    fun provideStorageApi(retrofit: Retrofit): SupabaseStorageApi = retrofit.create(SupabaseStorageApi::class.java)

    @Provides
    @Singleton
    fun provideListingsApi(retrofit: Retrofit): SupabaseListingsApi = retrofit.create(SupabaseListingsApi::class.java)

    @Provides
    @Singleton
    fun provideLandlordApi(retrofit: Retrofit): SupabaseLandlordApi = retrofit.create(SupabaseLandlordApi::class.java)

    @Provides
    @Singleton
    fun provideAdminApi(retrofit: Retrofit): SupabaseAdminApi = retrofit.create(SupabaseAdminApi::class.java)

    @Provides
    @Singleton
    fun provideAdminAccessApi(retrofit: Retrofit): SupabaseAdminAccessApi = retrofit.create(SupabaseAdminAccessApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationsApi(retrofit: Retrofit): SupabaseNotificationsApi = retrofit.create(SupabaseNotificationsApi::class.java)

    @Provides
    @Singleton
    fun provideSupportApi(retrofit: Retrofit): SupabaseSupportApi = retrofit.create(SupabaseSupportApi::class.java)

}
