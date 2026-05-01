package com.example.diplom.data

import retrofit2.http.GET
import retrofit2.http.Path

interface UserService {
    @GET("users/me")
    suspend fun getMyProfile(): User
    
    @GET("users/{id}")
    suspend fun getUserProfile(@Path("id") id: String): User
}
