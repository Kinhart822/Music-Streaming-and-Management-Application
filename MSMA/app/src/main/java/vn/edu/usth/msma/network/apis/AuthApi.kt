package vn.edu.usth.msma.network.apis

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import vn.edu.usth.msma.data.dto.request.auth.RefreshRequest
import vn.edu.usth.msma.data.dto.request.auth.SignInRequest
import vn.edu.usth.msma.data.dto.response.auth.RefreshResponse
import vn.edu.usth.msma.data.dto.response.auth.SignInResponse

interface AuthApi {
    @POST("/api/v1/auth/sign-in")
    suspend fun signIn(@Body request: SignInRequest): Response<SignInResponse>

    @PUT("/api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<RefreshResponse>

    @PUT("/api/v1/auth/sign-out")
    suspend fun signOut(): Response<Unit>
}
