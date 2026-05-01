package com.example.diplom.data

import retrofit2.http.Body
import retrofit2.http.POST

data class AiGenerateRequest(
    val products: List<String>
)

data class AiGenerateResponse(
    val recipe_text: String
)

interface ChatService {
    @POST("ai/generate")
    suspend fun generateRecipe(@Body request: AiGenerateRequest): AiGenerateResponse
}
