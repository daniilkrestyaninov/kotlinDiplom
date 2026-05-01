package com.example.diplom.data

import com.google.gson.annotations.SerializedName

data class Recipe(
    val id: String,
    val title: String,
    val description: String?,
    val difficulty: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("cooking_time") val cookingTime: Int?,
    @SerializedName("Likes") val likes: List<RecipeLike>? = emptyList(),
    // We keep these for optimistic UI updates:
    @SerializedName("likes_count") var likesCount: Int? = null,
    @SerializedName("comments_count") var commentsCount: Int? = null,
    @SerializedName("is_liked") var isLiked: Boolean? = null,
    val User: User?
)

data class RecipeLike(
    @SerializedName("user_id") val userId: String
)

data class User(
    val id: String,
    val username: String,
    val name: String?,
    @SerializedName("avatar_url") val avatarUrl: String?
)

data class Category(
    val id: String,
    val name: String,
    val description: String?
)
