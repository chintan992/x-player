package com.chintan992.xplayer

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeaderStorage @Inject constructor() {

    private val headers = ConcurrentHashMap<String, Map<String, String>>()

    fun addHeaders(host: String, headersToAdd: Map<String, String>) {
        headers[host] = headersToAdd
    }

    fun getHeaders(host: String): Map<String, String>? {
        return headers[host]
    }

    fun clearHeaders(host: String) {
        headers.remove(host)
    }

    fun clearAll() {
        headers.clear()
    }
}
