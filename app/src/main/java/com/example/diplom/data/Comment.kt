package com.example.diplom.data

import com.google.gson.annotations.SerializedName

data class Comment(
    val id: String,
    val content: String,
    val rating: Int?,
    @SerializedName("taste_sweet") val tasteSweet: Int? = null,
    @SerializedName("taste_sour") val tasteSour: Int? = null,
    @SerializedName("taste_salty") val tasteSalty: Int? = null,
    @SerializedName("taste_spicy") val tasteSpicy: Int? = null,
    @SerializedName("taste_umami") val tasteUmami: Int? = null,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("Author") val author: User?,
    @SerializedName("Replies") val replies: List<Comment>? = emptyList()
)

data class CommentRequest(
    val content: String,
    val rating: Int?,
    @SerializedName("taste_sweet") val tasteSweet: Int? = null,
    @SerializedName("taste_sour") val tasteSour: Int? = null,
    @SerializedName("taste_salty") val tasteSalty: Int? = null,
    @SerializedName("taste_spicy") val tasteSpicy: Int? = null,
    @SerializedName("taste_umami") val tasteUmami: Int? = null
)
