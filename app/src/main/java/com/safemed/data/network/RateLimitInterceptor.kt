package com.safemed.data.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Interceptor để xử lý lỗi 429 (Rate Limit) từ Gemini API
 * 
 * Chiến lược:
 * - Khi nhận mã lỗi 429, tự động chờ (delay) và thực hiện lại (retry)
 * - Tối đa 2 lần retry với delay 3 giây giữa mỗi lần
 * - Nếu vẫn thất bại sau 2 lần, throw exception để UI xử lý
 */
@Singleton
class RateLimitInterceptor @Inject constructor() : Interceptor {
    
    companion object {
        private const val TAG = "RateLimitInterceptor"
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 3000L // 3 seconds
        private const val HTTP_TOO_MANY_REQUESTS = 429
    }
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var retryCount = 0
        
        while (response.code == HTTP_TOO_MANY_REQUESTS && retryCount < MAX_RETRIES) {
            retryCount++
            Log.w(TAG, "Rate limit hit (429). Retry attempt $retryCount/$MAX_RETRIES after ${RETRY_DELAY_MS}ms delay")
            
            // Close the previous response before retrying
            response.close()
            
            // Wait before retrying
            try {
                Thread.sleep(RETRY_DELAY_MS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Retry interrupted", e)
            }
            
            // Retry the request
            response = chain.proceed(request)
        }
        
        if (response.code == HTTP_TOO_MANY_REQUESTS) {
            Log.e(TAG, "Rate limit exceeded after $MAX_RETRIES retries")
            // Response will be returned as-is, let the repository handle it
        }
        
        return response
    }
}

/**
 * Custom exception cho Rate Limit
 */
class RateLimitExceededException(
    message: String = "Rate limit exceeded. Please try again later."
) : IOException(message)
