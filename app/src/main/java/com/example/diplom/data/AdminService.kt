package com.example.diplom.data

import retrofit2.http.*

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

interface AdminService {
    @GET("admin/menu-of-week")
    suspend fun getMenuOfTheWeek(): List<MenuOfTheWeekItem>

    @POST("admin/menu-of-week")
    suspend fun addToMenu(@Body body: Map<String, Any>)

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

    @GET("reports")
    suspend fun getReports(): List<ReportItem>

    @PATCH("reports/{id}")
    suspend fun updateReportStatus(@Path("id") id: Long, @Body body: Map<String, String>)
}

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
    val Reporter: UserBrief?,
    val ReportedUser: UserBrief?,
    val ReportedRecipe: RecipeBrief?
)

data class RecipeBrief(
    val id: Long,
    val title: String
)
