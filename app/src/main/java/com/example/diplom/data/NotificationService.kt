package com.example.diplom.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.Response

data class NotificationResponse(
    val notifications: List<Notification>,
    val pagination: NotificationPagination
)

data class NotificationPagination(
    val total: Int,
    val page: Int,
    val limit: Int,
    @SerializedName("totalPages") val totalPages: Int
)

data class UnreadCountResponse(
    val count: Int
)

data class DeviceTokenRequest(
    val token: String,
    val device_type: String = "android"
)

interface NotificationService {
    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): NotificationResponse

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): UnreadCountResponse

    @PATCH("notifications/read-all")
    suspend fun markAllAsRead()

    @PATCH("notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: Int)

    @DELETE("notifications")
    suspend fun deleteAll()

    @POST("notifications/register-device")
    suspend fun registerDevice(@Body request: DeviceTokenRequest): Response<Unit>
}
