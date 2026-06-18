package com.example.diplom.data

import com.google.gson.annotations.SerializedName

enum class NotificationType {
    LIKE, FOLLOW, NEW_POST, COMMENT, REPLY, SYSTEM
}

data class NotificationActor(
    val id: String? = null,
    val username: String? = null,
    val name: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val role: String? = "user",
    @SerializedName("is_verified") val isVerified: Boolean? = false,
    @SerializedName("is_blocked") val isBlocked: Boolean? = false
)

data class Notification(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("actor_id") val actorId: Int?,
    val type: NotificationType,
    @SerializedName("recipe_id") val recipeId: Int? = null,
    @SerializedName("comment_id") val commentId: Int? = null,
    val message: String? = null,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String? = null,
    
    @SerializedName("Actor") val actor: NotificationActor? = null,
    @SerializedName("Recipe") val recipe: Recipe? = null,
    @SerializedName("Comment") val comment: Comment? = null
)
