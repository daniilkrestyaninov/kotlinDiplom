package com.example.diplom.data

import com.google.gson.annotations.SerializedName

data class Comment(
    val id: String,
    val text: String,
    val rating: Float?,
    @SerializedName("created_at") val createdAt: String?,
    val User: User?
)

data class CommentRequest(
    val text: String,
    val rating: Int?
)
