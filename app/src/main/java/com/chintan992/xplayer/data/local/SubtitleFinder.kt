package com.chintan992.xplayer.data.local

import android.net.Uri
import java.io.File
import javax.inject.Inject

class SubtitleFinder @Inject constructor() {

    fun findSubtitleForVideo(videoPath: String): Uri? {
        try {
            val videoFile = File(videoPath)
            val parentFile = videoFile.parentFile ?: return null
            val baseName = videoFile.nameWithoutExtension
            
            val extensions = arrayOf("srt", "ass", "ssa", "vtt")
            for (ext in extensions) {
                val subtitleFile = File(parentFile, "$baseName.$ext")
                if (subtitleFile.exists()) {
                    return Uri.fromFile(subtitleFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
