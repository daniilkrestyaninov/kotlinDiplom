package com.example.diplom.data

import com.google.gson.annotations.SerializedName

data class DietPlan(
    val id: Long? = null,
    @SerializedName("user_id") val userId: Long? = null,
    val title: String,
    val description: String?,
    @SerializedName("is_private") val isPrivate: Boolean,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    val Author: User? = null,
    @SerializedName("DayRecipes") val dayRecipes: List<DietPlanRecipe>? = null
)

data class DietPlanRecipe(
    val id: Long? = null,
    @SerializedName("diet_plan_id") val dietPlanId: Long? = null,
    @SerializedName("recipe_id") val recipeId: Long,
    @SerializedName("day_of_week") val dayOfWeek: Int, // 1-7 (Mon-Sun)
    @SerializedName("meal_order") val mealOrder: Int, // 1-5
    val Recipe: Recipe? = null
)

data class CreateDietPlanRequest(
    val title: String,
    val description: String?,
    @SerializedName("is_private") val isPrivate: Boolean,
    val recipes: List<DietPlanRecipeRequest>
)

data class DietPlanRecipeRequest(
    @SerializedName("recipe_id") val recipeId: Long,
    @SerializedName("day_of_week") val dayOfWeek: Int,
    @SerializedName("meal_order") val mealOrder: Int
)
