package vn.edu.usth.msma.network.apis

import retrofit2.Response
import retrofit2.http.*
import vn.edu.usth.msma.data.dto.request.auth.*
import vn.edu.usth.msma.data.dto.response.auth.*

interface AccountApi {
    @GET("/api/v1/account/user/sign-up/check-email-existence")
    suspend fun checkEmailExistence(@Query("query") query: String): Response<EmailExistenceResponse>

    @POST("/api/v1/account/signUpUser")
    suspend fun signUpFinish(@Body request: SignUpRequest): Response<ApiResponse>

    @POST("/api/v1/account/user/forgot-password/begin")
    suspend fun forgotPasswordBegin(@Body request: SendOtpRequest): Response<OtpDueDateResponse>

    @POST("/api/v1/account/user/forgot-password/check-otp")
    suspend fun forgotPasswordCheckOtp(@Body request: CheckOtpRequest): Response<OtpCheckResultResponse>

    @POST("/api/v1/account/user/forgot-password/finish")
    suspend fun forgotPasswordFinish(@Body request: NewPasswordRequest): Response<ApiResponse>
}
