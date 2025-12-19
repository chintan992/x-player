package com.chintan992.xplayer

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class CustomHeaderInterceptor @Inject constructor(
    private val headerStorage: HeaderStorage
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        val headers = headerStorage.getHeaders(host)

        val newRequest = if (headers != null) {
            val builder = request.newBuilder()
            for ((key, value) in headers) {
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
