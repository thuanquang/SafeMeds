package com.safemed.data.network

import com.safemed.data.model.GeminiRequest
import com.safemed.data.model.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface cho Gemini API
 * 
 * Base URL: https://generativelanguage.googleapis.com/
 * Model: gemini-2.5-flash (5 RPM free tier, nhanh và phù hợp cho chat)
 * 
 * Reference: https://ai.google.dev/api/generate-content
 */
interface GeminiApiService {
    
    /**
     * Gọi API sinh nội dung với Gemini 2.5 Flash
     * 
     * @param apiKey API key từ Google AI Studio
     * @param request Request body chứa contents, systemInstruction, generationConfig
     * @return GeminiResponse chứa nội dung phản hồi hoặc lỗi
     */
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
