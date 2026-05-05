package com.example.diplom.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

data class ParseRequest(
    val url: String
)

data class ParsedRecipe(
    val title: String?,
    val description: String?,
    val ingredients: List<ParsedIngredient>?,
    val steps: List<ParsedStep>?
)

data class ParsedIngredient(
    val name: String?,
    val quantity: String?,
    val unit: String?
)

data class ParsedStep(
    @SerializedName("step_number") val stepNumber: Int?,
    val description: String?
)

data class ParseResponse(
    val message: String?,
    val parsed: ParsedRecipe?
)

interface ToolsService {
    @POST("tools/parse")
    suspend fun parseRecipe(@Body request: ParseRequest): ParseResponse
}
