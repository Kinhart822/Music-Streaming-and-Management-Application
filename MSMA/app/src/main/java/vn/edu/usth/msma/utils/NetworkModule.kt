package vn.edu.usth.msma.utils

import android.content.Context
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.network.ApiClient
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.network.CustomAuthenticator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideApiClient(
        preferencesManager: PreferencesManager,
        customAuthenticator: CustomAuthenticator
    ): ApiClient {
        return ApiClient(preferencesManager, customAuthenticator)
    }

    @Provides
    @Singleton
    fun provideApiService(apiClient: ApiClient): ApiService {
        return ApiService(apiClient)
    }

    @Provides
    @Singleton
    fun provideCustomAuthenticator(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager,
        apiService: Lazy<ApiService>
    ): CustomAuthenticator {
        return CustomAuthenticator(context, preferencesManager, apiService)
    }
}