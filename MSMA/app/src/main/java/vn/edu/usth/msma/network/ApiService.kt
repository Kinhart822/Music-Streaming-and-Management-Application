package vn.edu.usth.msma.network

import android.content.Context
import vn.edu.usth.msma.network.apis.AccountApi
import vn.edu.usth.msma.network.apis.AuthApi

object ApiService {
    private var unAuthApi: AuthApi? = null
    private var authApi: AuthApi? = null
    private var accountApi: AccountApi? = null

    fun getUnAuthApi(): AuthApi {
        if (unAuthApi == null) {
            unAuthApi = ApiClient.getUnauthenticatedClient().create(AuthApi::class.java)
        }
        return unAuthApi!!
    }

    fun getAuthApi(context: Context): AuthApi {
        if (authApi == null) {
            authApi = ApiClient.getAuthenticatedClient(context).create(AuthApi::class.java)
        }
        return authApi!!
    }

    fun getUnAccountApi(): AccountApi {
        if (accountApi == null) {
            accountApi = ApiClient.getUnauthenticatedClient().create(AccountApi::class.java)
        }
        return accountApi!!
    }

    fun getAccountApi(context: Context): AccountApi {
        if (accountApi == null) {
            accountApi = ApiClient.getAuthenticatedClient(context).create(AccountApi::class.java)
        }
        return accountApi!!
    }

    fun resetApis() {
        unAuthApi = null
        authApi = null
        accountApi = null
    }
}