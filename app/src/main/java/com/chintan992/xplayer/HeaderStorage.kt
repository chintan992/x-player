package com.chintan992.xplayer

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeaderStorage @Inject constructor() {
    private val headers = mutableMapOf<String, Map<String, String>>()

    fun addHeaders(host: String, headers: Map<String, String>) {
        this.headers[host] = headers
    }

    fun getHeaders(host: String): Map<String, String>? {
        return headers[host]
    }
}
