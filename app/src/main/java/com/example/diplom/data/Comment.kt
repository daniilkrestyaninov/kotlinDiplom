package com.example.diplom.data

import com.google.gson.annotations.SerializedName

data class Comment(
    val id: String,
    val content: String,
    val rating: Int?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("Author") val author: User?,
    @SerializedName("Replies") val replies: List<Comment>? = emptyList()
)

data class CommentRequest(
    val content: String,
    val rating: Int?
)
