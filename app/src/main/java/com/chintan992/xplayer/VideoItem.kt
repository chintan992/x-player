package com.chintan992.xplayer

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val duration: Long,
    val size: Long,
    val dateModified: Long = 0L,
    val folderPath: String = "",
    val folderName: String = "",
    val subtitleUri: Uri? = null
)

data class VideoFolder(
    val path: String,
    val name: String,
    val videoCount: Int,
    val totalSize: Long = 0L,
    val thumbnailUri: Uri? = null
)
