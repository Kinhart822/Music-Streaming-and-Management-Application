package vn.edu.usth.msma.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.data.dto.request.auth.RefreshRequest
import java.io.IOException

class CustomAuthenticator(
    private val context: Context,
    private val preferencesManager: PreferencesManager?
) : Authenticator {
    private var callback: (() -> Unit)? = null
    private val tag: String = javaClass.simpleName

    fun setCallback(callback: () -> Unit) {
        this.callback = callback
    }

    @Throws(IOException::class)
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code == 401 || response.code == 403) {
            Log.d(tag, "authenticate: need refresh")
            synchronized(this) {
                return runBlocking {
                    try {
                        val refreshToken = preferencesManager?.refreshToken?.first()
                        if (refreshToken == null) {
                            Log.d(tag, "authenticate: refresh token is null, navigating to sign in screen")
                            preferencesManager?.clearAll()
                            callback?.invoke()
                            return@runBlocking null
                        }
                        val email = preferencesManager.email.first() ?: ""
                        val refreshResponse = withContext(Dispatchers.IO) {
                            ApiService.getUnAuthApi().refresh(RefreshRequest(refreshToken, email))
                        }

                        if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                            val newAccessToken = refreshResponse.body()!!.accessToken
                            preferencesManager.apply {
                                removeAccessToken()
                                saveAccessToken(newAccessToken)
                            }
                            Log.d(tag, "authenticate: accessToken is refreshed")
                            response.request.newBuilder()
                                .header("Authorization", "Bearer $newAccessToken")
                                .build()
                        } else if (refreshResponse.code() == 401) {
                            Log.d(tag, "authenticate: refresh token expired")
                            preferencesManager.clearAll()
                            callback?.invoke()
                            null
                        } else {
                            Log.d(tag, "authenticate: refresh failed with code ${refreshResponse.code()}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "authenticate: error refreshing token", e)
                        preferencesManager?.clearAll()
                        callback?.invoke()
                        null
                    }
                }
            }
        }
        return null
    }
}