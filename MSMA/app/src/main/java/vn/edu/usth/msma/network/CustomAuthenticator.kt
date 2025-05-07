package vn.edu.usth.msma.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.data.dto.request.auth.RefreshRequest
import java.io.IOException
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
import com.google.gson.JsonObject
import vn.edu.usth.msma.utils.eventbus.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import vn.edu.usth.msma.utils.eventbus.Event.SessionExpiredEvent

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
        val responseBody = response.body?.string()
        val isJwtExpired = responseBody?.let {
            try {
                val json = JSONObject(it)
                json.has("message") && json.getString("message").contains("JWT expired") ||
                        json.has("description") && json.getString("description").contains("JWT expired")
            } catch (e: Exception) {
                Log.e(tag, "Error parsing response body", e)
                false
            }
        } == true

        if (response.code == 401 || response.code == 403) {
            Log.d(tag, "authenticate: need refresh due to ${response.code} or JWT expired")
            synchronized(this) {
                return runBlocking {
                    try {
                        val email = preferencesManager.currentUserEmailFlow.first() ?: run {
                            Log.d(tag, "authenticate: no current user, navigating to Login screen")
                            preferencesManager.logout()
                            showSessionExpiredToast()
                            callback?.invoke()
                            return@runBlocking null
                        }
                        val refreshToken = preferencesManager.getRefreshTokenFlow(email).first()
                        if (refreshToken == null) {
                            Log.d(tag, "authenticate: refresh token is null, navigating to Login screen")
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
                            Log.d(tag, "authenticate: accessToken is refreshed")
                            response.request.newBuilder()
                                .header("Authorization", "Bearer $newAccessToken")
                                .build()
                        } else if (refreshResponse.code() == 401) {
                            Log.d(tag, "authenticate: refresh token expired")
                            preferencesManager.logout()
                            showSessionExpiredToast()
                            callback?.invoke()
                            null
                        } else {
                            Log.d(tag, "authenticate: refresh failed with code ${refreshResponse.code()}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "authenticate: error refreshing token", e)
                        preferencesManager.logout()
                        showSessionExpiredToast()
                        callback?.invoke()
                        null
                    }
                }
            }
        }
        return null
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

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Check if response is error and contains JWT expired message
        if (!response.isSuccessful) {
            val responseBody = response.body?.string()
            try {
                val jsonObject = Gson().fromJson(responseBody, JsonObject::class.java)
                val message = jsonObject.get("message")?.asString
                val description = jsonObject.get("description")?.asString

                if (message == "INTERNAL_SERVER_ERROR" || 
                    description?.contains("JWT expired") == true) {
                    Log.d("CustomAuthenticator", "JWT expired, clearing session")
                    // Clear user session and publish event in coroutine
                    scope.launch {
                        preferencesManager.logout()
                        EventBus.publish(SessionExpiredEvent)
                    }
                }
            } catch (e: Exception) {
                Log.e("CustomAuthenticator", "Error parsing response: ${e.message}")
            }
        }

        return response
    }
}