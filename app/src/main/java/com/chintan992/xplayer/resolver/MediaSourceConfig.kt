package com.chintan992.xplayer.resolver

data class MediaSourceConfig(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<SubtitleConfig> = emptyList()
)

data class SubtitleConfig(
    val url: String,
    val language: String,
    val mimeType: String,
    val isAutoSelected: Boolean = false
)
