package com.example.diplom.data

import retrofit2.http.Body
import retrofit2.http.POST

data class AiGenerateRequest(
    val products: List<String>
)

data class AiRecipeSuggestion(
    val title: String,
    val description: String,
    val ingredients: List<AiIngredient>,
    val steps: List<AiStep>
)

data class AiIngredient(
    val name: String,
    val quantity: String,
    val unit: String
)

data class AiStep(
    val step_number: Int,
    val description: String
)

data class AiGenerateResponse(
    val message: String,
    val suggestion: AiRecipeSuggestion?
)

interface ChatService {
    @POST("ai/generate")
    suspend fun generateRecipe(@Body request: AiGenerateRequest): AiGenerateResponse
}
