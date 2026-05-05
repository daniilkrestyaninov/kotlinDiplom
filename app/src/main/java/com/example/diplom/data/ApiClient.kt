package com.example.diplom.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://188.233.238.70:5000/"
    
    private var tokenManager: TokenManager? = null

    fun init(manager: TokenManager) {
        tokenManager = manager
    }

    private val authInterceptor = Interceptor { chain ->
        try {
            val requestBuilder = chain.request().newBuilder()
            
            // Get token synchronously for the request
            val token = runBlocking { tokenManager?.getToken?.first() }
            
            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            
            chain.proceed(requestBuilder.build())
        } catch (e: Exception) {
            throw java.io.IOException(e.message ?: "Network error")
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val recipeService: RecipeService by lazy { retrofit.create(RecipeService::class.java) }
    val authService: AuthService by lazy { retrofit.create(AuthService::class.java) }
    val userService: UserService by lazy { retrofit.create(UserService::class.java) }
    val chatService: ChatService by lazy { retrofit.create(ChatService::class.java) }
    val toolsService: ToolsService by lazy { retrofit.create(ToolsService::class.java) }
}
