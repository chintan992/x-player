package com.chintan992.xplayer

import java.util.concurrent.ConcurrentHashMap

object HeaderStorage {
    private val headers = ConcurrentHashMap<String, Map<String, String>>()

    fun addHeaders(host: String, headersToAdd: Map<String, String>) {
        headers[host] = headersToAdd
    }

    fun getHeaders(host: String): Map<String, String>? {
        return headers[host]
    }
}
