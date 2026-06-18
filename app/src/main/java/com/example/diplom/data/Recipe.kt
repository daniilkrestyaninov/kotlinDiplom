package com.example.diplom.data

import com.google.gson.annotations.SerializedName

data class Recipe(
    val id: Long,
    val title: String,
    val description: String?,
    val difficulty: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("cooking_time") val cookingTime: Int?,
    val portion: Int? = null,
    val calorific: Int? = null,
    val proteins: Float? = null,
    val fats: Float? = null,
    val carbohydrates: Float? = null,
    @SerializedName("is_private") val isPrivate: Boolean? = false,
    @SerializedName("is_generated") val isGenerated: Boolean? = false,
    @SerializedName("is_parsed") val isParsed: Boolean? = false,
    @SerializedName("kitchen_id") val kitchenId: String? = null,
    @SerializedName("celebration_id") val celebrationId: String? = null,
    @SerializedName("cooking_id") val cookingId: String? = null,
    @SerializedName("Likes") val likes: List<RecipeLike>? = emptyList(),
    @SerializedName("Steps") val steps: List<RecipeStep>? = emptyList(),
    @SerializedName("Ingredients") val ingredients: List<RecipeIngredient>? = emptyList(),
    @SerializedName("Categories") val categories: List<Category>? = emptyList(),
    @SerializedName("Kitchen") val kitchen: Category? = null,
    @SerializedName("Celebration") val celebration: Category? = null,
    @SerializedName("TypeCooking") val typeCooking: Category? = null,
    // For optimistic UI updates:
    @SerializedName("likes_count") var likesCount: Int? = null,
    @SerializedName("comments_count") var commentsCount: Int? = null,
    @SerializedName("views_count") var viewsCount: Int? = null,
    var rating: Double? = null,
    @SerializedName("is_liked") var isLiked: Boolean? = null,
    @SerializedName("is_cooked") var isCooked: Boolean? = null,
    var isFavorited: Boolean? = null,
    @SerializedName("cooked_count") var cookedCount: Int? = null,
    @SerializedName("personal_note") var personalNote: String? = null,
    @SerializedName("taste_averages") val tasteAverages: TasteAverages? = null,
    val User: User? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class TasteAverages(
    val sweet: String?,
    val sour: String?,
    val salty: String?,
    val spicy: String?,
    val umami: String?
)

data class RecipeLike(
    @SerializedName("user_id") val userId: String,
    @SerializedName("User") val user: User? = null
)

data class RecipeStep(
    val id: String? = null,
    @SerializedName("step_number") val stepNumber: Int,
    val description: String,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class RecipeIngredient(
    val id: String? = null,
    val name: String? = null,
    @SerializedName("RecipeIngredient") val pivot: IngredientPivot? = null
)

data class IngredientPivot(
    val quantity: String? = null,
    val unit: String? = null,
    val note: String? = null
)

data class User(
    val id: String,
    val username: String,
    val name: String?,
    val email: String? = null,
    val bio: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("is_following") var isFollowing: Boolean? = null,
    val role: String? = "user",
    @SerializedName("is_verified") val isVerified: Boolean? = false,
    @SerializedName("is_blocked") val isBlocked: Boolean? = false
)

data class Category(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null
)

// Request models for creating recipes
data class CreateRecipeRequest(
    val title: String,
    val description: String,
    val difficulty: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("cooking_time") val cookingTime: Int,
    val portion: Int,
    val calorific: Int? = null,
    val proteins: Float? = null,
    val fats: Float? = null,
    val carbohydrates: Float? = null,
    @SerializedName("is_private") val isPrivate: Boolean = false,
    @SerializedName("is_generated") val isGenerated: Boolean = false,
    @SerializedName("is_parsed") val isParsed: Boolean = false,
    @SerializedName("kitchen_id") val kitchenId: Int? = null,
    @SerializedName("celebration_id") val celebrationId: Int? = null,
    @SerializedName("cooking_id") val cookingId: Int? = null,
    val ingredients: List<IngredientInput> = emptyList(),
    val steps: List<StepInput> = emptyList(),
    val categories: List<Int> = emptyList()
)

data class IngredientInput(
    val id: Int? = null,
    val name: String? = null,
    val quantity: String? = null,
    val unit: String? = null,
    val note: String? = null
)

data class StepInput(
    @SerializedName("step_number") val stepNumber: Int,
    val description: String,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class Ingredient(
    val id: String,
    val name: String
)

data class UploadResponse(
    val message: String,
    val url: String,
    val fileName: String
)

object AiDraft {
    var suggestion: AiRecipeSuggestion? = null
}
