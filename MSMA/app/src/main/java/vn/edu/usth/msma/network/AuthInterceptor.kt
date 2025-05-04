package vn.edu.usth.msma.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import vn.edu.usth.msma.data.PreferencesManager
import java.io.IOException

class AuthInterceptor(private val authPrefsManager: PreferencesManager) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = runBlocking { authPrefsManager.accessToken.first() }
        val originalRequest = chain.request()
        if (accessToken == null) {
            return chain.proceed(originalRequest)
        }
        val newRequest = originalRequest
            .newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
        return chain.proceed(newRequest)
    }
}