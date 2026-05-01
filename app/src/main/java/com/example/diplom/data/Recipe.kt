package com.example.diplom.data

import com.google.gson.annotations.SerializedName

data class Recipe(
    val id: String,
    val title: String,
    val description: String?,
    val difficulty: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("cooking_time") val cookingTime: Int?,
    val User: User?
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
