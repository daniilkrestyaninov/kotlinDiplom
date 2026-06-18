package com.example.diplom.data

import retrofit2.http.Body
import retrofit2.http.POST

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val name: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    @SerializedName("access_token") val token: String,
    val user: User
)

data class RegisterResponse(
    val message: String,
    val userId: Int
)

data class VerifyEmailRequest(
    val email: String,
    val code: String
)

data class VerifyEmailResponse(
    val message: String
)

interface AuthService {
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): VerifyEmailResponse

    @POST("auth/resend-code")
    suspend fun resendCode(@Body request: Map<String, String>): Map<String, String>

    @POST("auth/password-recovery")
    suspend fun passwordRecovery(@Body request: Map<String, String>): Map<String, String>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: Map<String, String>): Map<String, String>
}
