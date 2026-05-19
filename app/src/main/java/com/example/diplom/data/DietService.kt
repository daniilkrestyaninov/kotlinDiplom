package com.example.diplom.data

import retrofit2.http.*

interface DietService {
    @POST("diet-plans")
    suspend fun createDietPlan(@Body request: CreateDietPlanRequest): DietPlan

    @GET("diet-plans")
    suspend fun getPublicDietPlans(@Query("search") search: String? = null): List<DietPlan>

    @GET("diet-plans/me")
    suspend fun getMyDietPlans(): List<DietPlan>

    @GET("diet-plans/{id}")
    suspend fun getDietPlanById(@Path("id") id: String): DietPlan

    @PATCH("diet-plans/{id}")
    suspend fun updateDietPlan(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any>): DietPlan

    @DELETE("diet-plans/{id}")
    suspend fun deleteDietPlan(@Path("id") id: String)
}
