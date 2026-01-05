package com.safemed.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request body cho Gemini API
 * Reference: https://ai.google.dev/api/generate-content
 */
data class GeminiRequest(
    @SerializedName("contents")
    val contents: List<GeminiContent>,
    
    @SerializedName("systemInstruction")
    val systemInstruction: GeminiContent? = null,
    
    @SerializedName("generationConfig")
    val generationConfig: GenerationConfig? = null,
    
    @SerializedName("safetySettings")
    val safetySettings: List<SafetySetting>? = null
)

/**
 * Nội dung tin nhắn (user hoặc model)
 */
data class GeminiContent(
    @SerializedName("role")
    val role: String? = null, // "user" hoặc "model"
    
    @SerializedName("parts")
    val parts: List<GeminiPart>
)

/**
 * Phần nội dung (text, image, etc.)
 */
data class GeminiPart(
    @SerializedName("text")
    val text: String
)

/**
 * Cấu hình sinh nội dung
 */
data class GenerationConfig(
    @SerializedName("temperature")
    val temperature: Float = 0.7f,
    
    @SerializedName("topK")
    val topK: Int = 40,
    
    @SerializedName("topP")
    val topP: Float = 0.95f,
    
    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int = 1024,
    
    @SerializedName("stopSequences")
    val stopSequences: List<String>? = null
)

/**
 * Cài đặt an toàn
 */
data class SafetySetting(
    @SerializedName("category")
    val category: String,
    
    @SerializedName("threshold")
    val threshold: String
)

/**
 * Response từ Gemini API
 */
data class GeminiResponse(
    @SerializedName("candidates")
    val candidates: List<Candidate>? = null,
    
    @SerializedName("promptFeedback")
    val promptFeedback: PromptFeedback? = null,
    
    @SerializedName("error")
    val error: GeminiError? = null
)

/**
 * Candidate response
 */
data class Candidate(
    @SerializedName("content")
    val content: GeminiContent? = null,
    
    @SerializedName("finishReason")
    val finishReason: String? = null,
    
    @SerializedName("safetyRatings")
    val safetyRatings: List<SafetyRating>? = null
)

/**
 * Feedback về prompt
 */
data class PromptFeedback(
    @SerializedName("blockReason")
    val blockReason: String? = null,
    
    @SerializedName("safetyRatings")
    val safetyRatings: List<SafetyRating>? = null
)

/**
 * Đánh giá an toàn
 */
data class SafetyRating(
    @SerializedName("category")
    val category: String,
    
    @SerializedName("probability")
    val probability: String
)

/**
 * Lỗi từ API
 */
data class GeminiError(
    @SerializedName("code")
    val code: Int? = null,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("status")
    val status: String? = null
)

/**
 * Extension function để lấy text từ response
 */
fun GeminiResponse.getResponseText(): String? {
    return candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
}

/**
 * Extension function để kiểm tra response có lỗi không
 */
fun GeminiResponse.hasError(): Boolean {
    return error != null || promptFeedback?.blockReason != null
}

/**
 * Extension function để lấy thông báo lỗi
 */
fun GeminiResponse.getErrorMessage(): String? {
    return error?.message ?: promptFeedback?.blockReason
}
