package com.example.diplom.data

import retrofit2.http.*

interface ReportService {
    @POST("reports")
    suspend fun createReport(@Body request: ReportRequest)

    @GET("reports")
    suspend fun getAllReports(): List<Report>

    @PATCH("reports/{id}")
    suspend fun updateReportStatus(@Path("id") id: Long, @Body status: Map<String, String>)
}
