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

interface AuthService {
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
}
