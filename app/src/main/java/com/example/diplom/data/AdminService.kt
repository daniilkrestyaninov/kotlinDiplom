package com.example.diplom.data

import retrofit2.http.*
import com.google.gson.annotations.SerializedName

data class MenuOfTheWeekItem(
    val id: Long,
    val day_of_week: Int,
    val recipe_id: Long,
    val Recipe: Recipe? = null
)

data class AuditLog(
    val id: Long,
    val admin_id: Long,
    val action: String,
    val entity: String?,
    val entity_id: Long?,
    val details: Any?,
    val created_at: String?
)

data class VerificationRequest(
    val id: Long,
    val user_id: Long,
    val full_name: String,
    val info: String?,
    val status: String,
    val User: UserBrief? = null
)

data class UserBrief(
    val id: Long,
    val username: String,
    val name: String,
    val avatar_url: String?
)

data class AddMenuRequest(
    @SerializedName("day_of_week") val dayOfWeek: Int,
    @SerializedName("recipe_id") val recipeId: Long
)

interface AdminService {
    @GET("admin/menu-of-week")
    suspend fun getMenuOfTheWeek(): List<MenuOfTheWeekItem>

    @POST("admin/menu-of-week")
    suspend fun addToMenu(@Body body: AddMenuRequest): MenuOfTheWeekItem

    @DELETE("admin/menu-of-week/{id}")
    suspend fun removeFromMenu(@Path("id") id: Long)

    @GET("admin/audit-logs")
    suspend fun getAuditLogs(): List<AuditLog>

    @GET("admin/verifications")
    suspend fun getVerifications(): List<VerificationRequest>

    @PATCH("admin/verifications/{id}")
    suspend fun updateVerification(@Path("id") id: Long, @Body body: Map<String, String>)
    
    @POST("admin/notifications/broadcast")
    suspend fun broadcastNotification(@Body body: Map<String, String>)

    @GET("admin/stats")
    suspend fun getStats(): Map<String, Any>

    @GET("admin/analytics")
    suspend fun getAnalytics(): Map<String, Any>

    @GET("admin/users")
    suspend fun getUsers(): List<User>

    @PATCH("admin/users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body request: AdminUpdateUserRequest): User

    @POST("admin/users/{id}/block")
    suspend fun blockUser(@Path("id") id: String)

    @POST("admin/users/{id}/unblock")
    suspend fun unblockUser(@Path("id") id: String)

    @GET("admin/appeals")
    suspend fun getAppeals(): List<AppealItem>

    @PATCH("admin/appeals/{id}")
    suspend fun updateAppealStatus(@Path("id") id: Long, @Body body: Map<String, String>)

    @GET("reports")
    suspend fun getReports(): List<ReportItem>

    @PATCH("reports/{id}")
    suspend fun updateReportStatus(@Path("id") id: Long, @Body body: Map<String, String>)

    // --- Categories CRUD ---
    @POST("meta/categories")
    suspend fun createCategory(@Body body: Map<String, String>): Category

    @PUT("meta/categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body body: Map<String, String>): Category

    @DELETE("meta/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Map<String, Any>

    // --- Kitchens CRUD ---
    @POST("meta/kitchens")
    suspend fun createKitchen(@Body body: Map<String, String>): Category

    @PUT("meta/kitchens/{id}")
    suspend fun updateKitchen(@Path("id") id: String, @Body body: Map<String, String>): Category

    @DELETE("meta/kitchens/{id}")
    suspend fun deleteKitchen(@Path("id") id: String): Map<String, Any>

    // --- Cooking Types CRUD ---
    @POST("meta/cooking-types")
    suspend fun createCookingType(@Body body: Map<String, String>): Category

    @PUT("meta/cooking-types/{id}")
    suspend fun updateCookingType(@Path("id") id: String, @Body body: Map<String, String>): Category

    @DELETE("meta/cooking-types/{id}")
    suspend fun deleteCookingType(@Path("id") id: String): Map<String, Any>

    // --- Celebrations CRUD ---
    @POST("meta/celebrations")
    suspend fun createCelebration(@Body body: Map<String, String>): Category

    @PUT("meta/celebrations/{id}")
    suspend fun updateCelebration(@Path("id") id: String, @Body body: Map<String, String>): Category

    @DELETE("meta/celebrations/{id}")
    suspend fun deleteCelebration(@Path("id") id: String): Map<String, Any>

    // --- Units CRUD ---
    @GET("meta/units")
    suspend fun getUnits(): List<UnitModel>

    @POST("meta/units")
    suspend fun createUnit(@Body body: Map<String, String>): UnitModel

    @PUT("meta/units/{id}")
    suspend fun updateUnit(@Path("id") id: Long, @Body body: Map<String, String>): UnitModel

    @DELETE("meta/units/{id}")
    suspend fun deleteUnit(@Path("id") id: Long): Map<String, Any>

    // --- Ingredients CRUD ---
    @GET("meta/ingredients")
    suspend fun getIngredients(@Query("search") search: String? = null): List<IngredientModel>

    @POST("meta/ingredients")
    suspend fun createIngredient(@Body body: Map<String, @JvmSuppressWildcards Any>): IngredientModel

    @PUT("meta/ingredients/{id}")
    suspend fun updateIngredient(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any>): IngredientModel

    @DELETE("meta/ingredients/{id}")
    suspend fun deleteIngredient(@Path("id") id: String): Map<String, Any>
}

data class UnitModel(
    val id: Long,
    val name: String,
    @SerializedName("short_name") val shortName: String
)

data class IngredientModel(
    val id: String,
    val name: String,
    @SerializedName("unit_id") val unitId: Long?,
    val description: String?,
    @SerializedName("Unit") val Unit: UnitModel?
)

data class AdminUpdateUserRequest(
    val name: String?,
    val bio: String?,
    val role_id: Int?
)
data class ReportItem(
    val id: Long,
    val type: String,
    val reason: String,
    val description: String?,
    val status: String,
    val created_at: String,
    @SerializedName("Reporter") val Reporter: UserBrief?,
    @SerializedName("ReportedUser") val ReportedUser: UserBrief?,
    @SerializedName("ReportedRecipe") val ReportedRecipe: RecipeBrief?
)

data class RecipeBrief(
    val id: Long,
    val title: String
)

data class AppealItem(
    val id: Long,
    val user_id: Long,
    val message: String,
    val status: String,
    val admin_notes: String?,
    val created_at: String,
    val User: UserBrief? = null
)
