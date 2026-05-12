package com.example.diplom.data

import com.google.gson.annotations.SerializedName

enum class NotificationType {
    LIKE, FOLLOW, NEW_POST, COMMENT, REPLY
}

data class Notification(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("actor_id") val actorId: Int,
    val type: NotificationType,
    @SerializedName("recipe_id") val recipeId: Int? = null,
    @SerializedName("comment_id") val commentId: Int? = null,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String? = null,
    
    @SerializedName("Actor") val actor: User? = null,
    @SerializedName("Recipe") val recipe: Recipe? = null,
    @SerializedName("Comment") val comment: Comment? = null
)
