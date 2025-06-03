package vn.edu.usth.msma.network.apis

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query
import vn.edu.usth.msma.data.dto.request.auth.CheckOtpRequest
import vn.edu.usth.msma.data.dto.request.auth.NewPasswordRequest
import vn.edu.usth.msma.data.dto.request.auth.SendOtpRequest
import vn.edu.usth.msma.data.dto.request.auth.SignUpRequest
import vn.edu.usth.msma.data.dto.request.profile.ChangePasswordRequest
import vn.edu.usth.msma.data.dto.response.auth.ApiResponse
import vn.edu.usth.msma.data.dto.response.auth.EmailExistenceResponse
import vn.edu.usth.msma.data.dto.response.auth.OtpCheckResultResponse
import vn.edu.usth.msma.data.dto.response.auth.OtpDueDateResponse
import vn.edu.usth.msma.data.dto.response.management.HistoryListenResponse
import vn.edu.usth.msma.data.dto.response.management.NotificationResponse
import vn.edu.usth.msma.data.dto.response.profile.UserPresentation

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

    @GET("api/v1/account/profile/user")
    suspend fun getUserDetailByUser(): Response<UserPresentation>

    @Multipart
    @PUT("/api/v1/account/update")
    suspend fun updateAccount(
        @Part avatar: MultipartBody.Part?,
        @Part("description") description: RequestBody,
        @Part("firstName") firstName: RequestBody,
        @Part("lastName") lastName: RequestBody,
        @Part("gender") gender: RequestBody,
        @Part("dateOfBirth") dateOfBirth: RequestBody,
        @Part("phone") phone: RequestBody
    ): Response<ApiResponse>

    @DELETE("/api/v1/account/delete")
    suspend fun deleteAccount(): Response<ApiResponse>

    @POST("/api/v1/account/reset-password")
    suspend fun updatePassword(@Body changePasswordRequest: ChangePasswordRequest): Response<ApiResponse>

    @GET("/api/v1/account/viewHistoryListen")
    suspend fun viewHistoryListen(): Response<List<HistoryListenResponse>>

    @GET("/api/v1/account/notification")
    suspend fun getAllUserNotifications(): Response<List<NotificationResponse>>
}
