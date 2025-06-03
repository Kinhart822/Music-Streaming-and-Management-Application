package vn.edu.usth.msma.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.data.dto.request.auth.RefreshRequest
import vn.edu.usth.msma.utils.eventbus.Event.SessionExpiredEvent
import vn.edu.usth.msma.utils.eventbus.EventBus
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val lazyApiService: Lazy<ApiService>
) : Authenticator, Interceptor {
    private var callback: (() -> Unit)? = null
    private val tag: String = javaClass.simpleName
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun setCallback(callback: () -> Unit) {
        this.callback = callback
    }

    @Throws(IOException::class)
    override fun authenticate(route: Route?, response: Response): Request? {
        // Buffer the response body to allow multiple reads
        val responseBody = response.peekBody(Long.MAX_VALUE).string()
        return handleJwtExpiration(response, responseBody)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            val responseBody = response.peekBody(Long.MAX_VALUE).string()
            try {
                if (response.code == 500) {
                    Log.d(tag, "intercept: JWT expired in 500 response, attempting to refresh token")
                    val newRequest = handleJwtExpiration(response, responseBody)
                    if (newRequest != null) {
                        // Retry the request with the new token
                        return chain.proceed(newRequest)
                    } else {
                        // If refresh fails, clear session and notify
                        scope.launch {
                            preferencesManager.logout()
                            EventBus.publish(SessionExpiredEvent)
                        }
                        showSessionExpiredToast()
                        callback?.invoke()
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "intercept: Error parsing response: ${e.message}")
            }
        }

        return response
    }

    private fun handleJwtExpiration(response: Response, responseBody: String): Request? {
        if (response.code in listOf(401, 403, 500)) {
            Log.d(tag, "handleJwtExpiration: Need refresh due to ${response.code} and JWT expired")
            synchronized(this) {
                return runBlocking {
                    try {
                        val email = preferencesManager.currentUserEmailFlow.first() ?: run {
                            Log.d(tag, "handleJwtExpiration: No current user, navigating to Login screen")
                            preferencesManager.logout()
                            showSessionExpiredToast()
                            callback?.invoke()
                            return@runBlocking null
                        }
                        val refreshToken = preferencesManager.getRefreshTokenFlow(email).first()
                        if (refreshToken == null) {
                            Log.d(tag, "handleJwtExpiration: Refresh token is null, navigating to Login screen")
                            preferencesManager.logout()
                            showSessionExpiredToast()
                            callback?.invoke()
                            return@runBlocking null
                        }
                        val refreshResponse = withContext(Dispatchers.IO) {
                            lazyApiService.get().getUnAuthApi().refresh(RefreshRequest(refreshToken, email))
                        }

                        if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                            val newAccessToken = refreshResponse.body()!!.accessToken
                            preferencesManager.saveAccessToken(email, newAccessToken)
                            Log.d(tag, "handleJwtExpiration: Access token refreshed")
                            response.request.newBuilder()
                                .header("Authorization", "Bearer $newAccessToken")
                                .build()
                        } else if (refreshResponse.code() == 401) {
                            Log.d(tag, "handleJwtExpiration: Refresh token expired")
                            preferencesManager.logout()
                            showSessionExpiredToast()
                            callback?.invoke()
                            null
                        } else {
                            Log.d(tag, "handleJwtExpiration: Refresh failed with code ${refreshResponse.code()}")
                            preferencesManager.logout()
                            showSessionExpiredToast()
                            callback?.invoke()
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "handleJwtExpiration: Error refreshing token", e)
                        preferencesManager.logout()
                        showSessionExpiredToast()
                        callback?.invoke()
                        null
                    }
                }
            }
        } else {
            Log.d(tag, "handleJwtExpiration: No JWT expiration detected for response code ${response.code}, body: $responseBody")
            return null
        }
    }

    private fun showSessionExpiredToast() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Session expired. Please sign in again.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}