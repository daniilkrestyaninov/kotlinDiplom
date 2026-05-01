package com.example.diplom.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface RecipeService {
    @GET("recipes")
    suspend fun getRecipes(
        @retrofit2.http.Query("category_id") categoryId: String? = null,
        @retrofit2.http.Query("kitchen_id") kitchenId: String? = null,
        @retrofit2.http.Query("cooking_id") cookingId: String? = null,
        @retrofit2.http.Query("celebration_id") celebrationId: String? = null
    ): List<Recipe>

    @GET("meta/categories")
    suspend fun getCategories(): List<Category>

    @GET("meta/kitchens")
    suspend fun getKitchens(): List<Category> // Using same Category model as it's id+name

    @GET("meta/cooking-types")
    suspend fun getCookingTypes(): List<Category>

    @GET("meta/celebrations")
    suspend fun getCelebrations(): List<Category>

    companion object {
        private const val BASE_URL = "http://188.233.238.70:5000/"

        fun create(): RecipeService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RecipeService::class.java)
        }
    }
}
