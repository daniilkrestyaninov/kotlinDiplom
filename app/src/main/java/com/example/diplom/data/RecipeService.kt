package com.example.diplom.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface RecipeService {
    @GET("recipes")
    suspend fun getRecipes(
        @Query("category_id") categoryId: String? = null,
        @Query("kitchen_id") kitchenId: String? = null,
        @Query("cooking_id") cookingId: String? = null,
        @Query("celebration_id") celebrationId: String? = null,
        @Query("user_id") userId: String? = null,
        @Query("is_private") isPrivate: Boolean? = null,
        @Query("search") search: String? = null
    ): List<Recipe>

    @GET("recipes/recommendations")
    suspend fun getRecommendations(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<Recipe>

    @GET("recipes/{id}")
    suspend fun getRecipeById(@Path("id") id: String): Recipe

    @POST("recipes")
    suspend fun createRecipe(@Body request: CreateRecipeRequest): Recipe

    @PUT("recipes/{id}")
    suspend fun updateRecipe(@Path("id") id: String, @Body request: Map<String, @JvmSuppressWildcards Any>): Recipe

    @DELETE("recipes/{id}")
    suspend fun deleteRecipe(@Path("id") id: String)

    @POST("recipes/{id}/like")
    suspend fun likeRecipe(@Path("id") id: String)

    @DELETE("recipes/{id}/like")
    suspend fun unlikeRecipe(@Path("id") id: String)

    @GET("recipes/{id}/comments")
    suspend fun getComments(@Path("id") id: String): List<Comment>

    @POST("recipes/{id}/comments")
    suspend fun postComment(@Path("id") id: String, @Body request: CommentRequest)

    @POST("comments/{id}/like")
    suspend fun toggleCommentLike(@Path("id") id: String): Map<String, Boolean>

    @PATCH("recipes/{id}/personal-note")
    suspend fun updatePersonalNote(@Path("id") id: String, @Body body: Map<String, String>)

    @POST("recipes/{id}/cooked")
    suspend fun markCooked(@Path("id") id: String)

    @POST("recipes/{id}/favorite")
    suspend fun addToFavorite(@Path("id") id: String)

    @DELETE("recipes/{id}/favorite")
    suspend fun removeFromFavorite(@Path("id") id: String)

    @GET("meta/categories")
    suspend fun getCategories(): List<Category>

    @GET("meta/kitchens")
    suspend fun getKitchens(): List<Category>

    @GET("meta/cooking-types")
    suspend fun getCookingTypes(): List<Category>

    @GET("meta/celebrations")
    suspend fun getCelebrations(): List<Category>

    @GET("meta/ingredients")
    suspend fun getIngredients(@Query("search") search: String? = null): List<Ingredient>

    @Multipart
    @POST("upload")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("folder") folder: RequestBody
    ): UploadResponse
}
