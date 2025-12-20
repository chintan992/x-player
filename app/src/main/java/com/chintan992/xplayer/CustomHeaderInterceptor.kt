package com.chintan992.xplayer

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class CustomHeaderInterceptor @Inject constructor(
    private val headerStorage: HeaderStorage
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        val customHeaders = headerStorage.getHeaders(host)

        val newRequest = if (customHeaders != null) {
            val builder = request.newBuilder()
            for ((key, value) in customHeaders) {
                builder.addHeader(key, value)
            }
            builder.build()
        } else {
            request
        }

        var response = chain.proceed(newRequest)
        var tryCount = 0
        while (!response.isSuccessful && tryCount < 3) {
            if (response.code == 503 || response.code == 429) {
                tryCount++
                response.close()
                // Simple backoff
                try {
                    Thread.sleep(1000L * tryCount)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                response = chain.proceed(newRequest)
            } else {
                break
            }
        }

        return response
    }
}
