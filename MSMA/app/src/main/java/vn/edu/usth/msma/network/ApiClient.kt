package vn.edu.usth.msma.network

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import vn.edu.usth.msma.MainActivity
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.utils.constants.IP
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val TAG = "ApiClient"
    private val BASE_URL: String = IP.KINHART822.getIp().also {
        Log.d(TAG, "Using BASE_URL: $it")
    }
    private var authenticatedRetrofit: Retrofit? = null
    private var unauthenticatedRetrofit: Retrofit? = null

    fun getAuthenticatedClient(context: Context): Retrofit {
        if (authenticatedRetrofit == null) {
            val preferencesManager = PreferencesManager(context)
            val client = OkHttpClient.Builder()
                .authenticator(getCustomAuthenticator(context, preferencesManager))
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

    private fun getCustomAuthenticator(
        context: Context,
        preferencesManager: PreferencesManager?
    ): CustomAuthenticator {
        val authenticator = CustomAuthenticator(context, preferencesManager)
        authenticator.setCallback {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                Toast.makeText(
                    context,
                    "Session expired. Please sign in again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            handler.postDelayed({
                val intent = Intent(context, MainActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
            }, 2000)
        }
        return authenticator
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