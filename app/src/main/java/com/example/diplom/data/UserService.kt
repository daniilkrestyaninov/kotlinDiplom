package com.example.diplom.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// Full user profile response from GET /users/:id
data class UserProfile(
    val id: String,
    val username: String,
    val name: String?,
    val email: String?,
    val bio: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("Recipes") val recipes: List<Recipe>? = null,
    val stats: UserStats? = null
)

data class UserStats(
    val recipesCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0
)

// Favorite wrapper from GET /favorites
data class FavoriteItem(
    val id: String,
    @SerializedName("recipe_id") val recipeId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("is_downloaded") val isDownloaded: Boolean = false,
    @SerializedName("Recipe") val recipe: Recipe? = null
)

interface UserService {
    @GET("users/me")
    suspend fun getMyProfile(): User
    
    @GET("users/{id}")
    suspend fun getUserProfile(@Path("id") id: String): UserProfile

    // Subscriptions
    @POST("users/{id}/follow")
    suspend fun follow(@Path("id") id: String)

    @DELETE("users/{id}/follow")
    suspend fun unfollow(@Path("id") id: String)

    @GET("users/{id}/followers")
    suspend fun getFollowers(@Path("id") id: String): List<User>

    @GET("users/{id}/following")
    suspend fun getFollowing(@Path("id") id: String): List<User>

    // Favorites
    @GET("favorites")
    suspend fun getFavorites(): List<FavoriteItem>

    @POST("recipes/{id}/favorite")
    suspend fun addFavorite(@Path("id") recipeId: String)

    @DELETE("recipes/{id}/favorite")
    suspend fun removeFavorite(@Path("id") recipeId: String)
}
