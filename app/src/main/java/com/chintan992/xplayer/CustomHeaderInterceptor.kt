package com.chintan992.xplayer

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class CustomHeaderInterceptor : Interceptor {

    companion object {
        private val headers = ConcurrentHashMap<String, Map<String, String>>()

        fun addHeaders(host: String, headersToAdd: Map<String, String>) {
            headers[host] = headersToAdd
        }

        fun getHeaders(host: String): Map<String, String>? {
            return headers[host]
        }
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        val customHeaders = getHeaders(host)

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
                Thread.sleep(1000)
                response = chain.proceed(newRequest)
            } else {
                break
            }
        }

        return response
    }
}
