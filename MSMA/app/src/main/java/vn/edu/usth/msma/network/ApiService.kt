package vn.edu.usth.msma.network

import vn.edu.usth.msma.network.apis.AccountApi
import vn.edu.usth.msma.network.apis.AuthApi
import vn.edu.usth.msma.network.apis.SearchApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiService @Inject constructor(
    private val apiClient: ApiClient
) {
    private val _unAuthApi: AuthApi by lazy {
        apiClient.getUnauthenticatedClient().create(AuthApi::class.java)
    }

    private val _authApi: AuthApi by lazy {
        apiClient.getAuthenticatedClient().create(AuthApi::class.java)
    }

    private val _unAccountApi: AccountApi by lazy {
        apiClient.getUnauthenticatedClient().create(AccountApi::class.java)
    }

    private val _accountApi: AccountApi by lazy {
        apiClient.getAuthenticatedClient().create(AccountApi::class.java)
    }

    private val _searchApi: SearchApi by lazy {
        apiClient.getAuthenticatedClient().create(SearchApi::class.java)
    }

    fun getUnAuthApi(): AuthApi = _unAuthApi

    fun getAuthApi(): AuthApi = _authApi

    fun getUnAccountApi(): AccountApi = _unAccountApi

    fun getAccountApi(): AccountApi = _accountApi

    fun getSearchApi(): SearchApi = _searchApi
}