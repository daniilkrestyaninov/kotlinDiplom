package com.example.diplom.data

import com.google.gson.annotations.SerializedName

data class Report(
    val id: Long,
    @SerializedName("reporter_id") val reporterId: Long,
    val type: String, // recipe, user, profile
    @SerializedName("reported_user_id") val reportedUserId: Long?,
    @SerializedName("recipe_id") val recipeId: Long?,
    val reason: String,
    val description: String?,
    val status: String, // pending, reviewed, resolved, dismissed
    @SerializedName("created_at") val createdAt: String
)

data class ReportRequest(
    val type: String,
    @SerializedName("reported_user_id") val reportedUserId: Long? = null,
    @SerializedName("recipe_id") val recipeId: Long? = null,
    val reason: String,
    val description: String? = null
)
