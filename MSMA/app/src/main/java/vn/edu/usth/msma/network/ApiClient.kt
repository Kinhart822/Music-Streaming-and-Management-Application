package vn.edu.usth.msma.network

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.utils.constants.IP
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val customAuthenticator: CustomAuthenticator
) {
    private val TAG = "ApiClient"
    private val BASE_URL: String = IP.KINHART822_ZEN8LABS.getIp().also {
        Log.d(TAG, "Using BASE_URL: $it")
    }
    private var authenticatedRetrofit: Retrofit? = null
    private var unauthenticatedRetrofit: Retrofit? = null

    fun getAuthenticatedClient(): Retrofit {
        if (authenticatedRetrofit == null) {
            val client = OkHttpClient.Builder()
                .authenticator(customAuthenticator)
                .addInterceptor(AuthInterceptor(preferencesManager))
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
            authenticatedRetrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return authenticatedRetrofit!!
    }

    fun getUnauthenticatedClient(): Retrofit {
        if (unauthenticatedRetrofit == null) {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
            unauthenticatedRetrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return unauthenticatedRetrofit!!
    }

    fun resetClients() {
        authenticatedRetrofit = null
        unauthenticatedRetrofit = null
    }
}