package com.chintan992.xplayer.resolver

import kotlinx.coroutines.flow.Flow

interface StreamResolver {
    fun resolve(url: String): Flow<Resource<MediaSourceConfig>>
    
    // Pattern designed to identify if this resolver can handle the given URL
    fun canResolve(url: String): Boolean
}
