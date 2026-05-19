package com.example.diplom.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://umami-recipes.ru/api/"
    
    private var tokenManager: TokenManager? = null

    fun init(manager: TokenManager) {
        tokenManager = manager
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        
        // Get token synchronously
        try {
            val token = runBlocking { tokenManager?.getToken?.first() }
            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Token retrieval failed", e)
        }
        
        try {
            chain.proceed(requestBuilder.build())
        } catch (e: java.io.IOException) {
            android.util.Log.e("ApiClient", "Network call failed", e)
            okhttp3.Response.Builder()
                .request(chain.request())
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(503)
                .message("Service Unavailable")
                .body(okhttp3.ResponseBody.create(null, ""))
                .build()
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .retryOnConnectionFailure(true)
        .connectionPool(okhttp3.ConnectionPool(5, 30, java.util.concurrent.TimeUnit.SECONDS))
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
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
    val reportService: ReportService by lazy { retrofit.create(ReportService::class.java) }
    val notificationService: NotificationService by lazy { retrofit.create(NotificationService::class.java) }
    val adminService: AdminService by lazy { retrofit.create(AdminService::class.java) }
    val dietService: DietService by lazy { retrofit.create(DietService::class.java) }
}
