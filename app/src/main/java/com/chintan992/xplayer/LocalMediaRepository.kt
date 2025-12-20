package com.chintan992.xplayer

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LocalMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val validMimeTypes = arrayOf(
        "video/mp4",
        "video/x-matroska",  // MKV
        "video/webm",
        "video/avi",
        "video/x-msvideo",   // AVI alternative
        "video/3gpp",
        "video/quicktime"    // MOV
    )
    
    private val mimeTypeSelection = "${MediaStore.Video.Media.MIME_TYPE} IN (${validMimeTypes.joinToString { "?" }})"

    fun getVideos(): Flow<List<VideoItem>> = flow {
        val videoList = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            mimeTypeSelection,
            validMimeTypes,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val bucketColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn) ?: "Unknown"
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)
                val dateModified = it.getLong(dateModifiedColumn)
                val data = it.getString(dataColumn) ?: ""
                val folderName = it.getString(bucketColumn) ?: "Unknown"
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                
                // Extract folder path from full file path
                val folderPath = data.substringBeforeLast("/", "")
                val subtitleUri = findSubtitleForVideo(data)

                videoList.add(VideoItem(id, uri, name, duration, size, dateModified, folderPath, folderName, subtitleUri))
            }
        }

        emit(videoList)
    }.flowOn(Dispatchers.IO)

    fun getVideoFolders(): Flow<List<VideoFolder>> = flow {
        val folderMap = mutableMapOf<String, MutableList<VideoItem>>()
        
        // First get all videos
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            mimeTypeSelection,
            validMimeTypes,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val bucketColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn) ?: "Unknown"
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)
                val dateModified = it.getLong(dateModifiedColumn)
                val data = it.getString(dataColumn) ?: ""
                val folderName = it.getString(bucketColumn) ?: "Unknown"
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val folderPath = data.substringBeforeLast("/", "")
                val subtitleUri = findSubtitleForVideo(data)

                val video = VideoItem(id, uri, name, duration, size, dateModified, folderPath, folderName, subtitleUri)
                folderMap.getOrPut(folderPath) { mutableListOf() }.add(video)
            }
        }

        // Convert to folder list
        val folders = folderMap.map { (path, videos) ->
            VideoFolder(
                path = path,
                name = videos.firstOrNull()?.folderName ?: path.substringAfterLast("/"),
                videoCount = videos.size,
                totalSize = videos.sumOf { it.size },
                thumbnailUri = videos.firstOrNull()?.uri
            )
        }.sortedBy { it.name.lowercase() }

        emit(folders)
    }.flowOn(Dispatchers.IO)

    fun getVideosByFolder(folderPath: String): Flow<List<VideoItem>> = flow {
        val videoList = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        
        // Add folder path filter
        val selection = "$mimeTypeSelection AND ${MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = validMimeTypes + "$folderPath/%"

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val bucketColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn) ?: "Unknown"
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)
                val dateModified = it.getLong(dateModifiedColumn)
                val data = it.getString(dataColumn) ?: ""
                val folderName = it.getString(bucketColumn) ?: "Unknown"
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val videoFolderPath = data.substringBeforeLast("/", "")
                val subtitleUri = findSubtitleForVideo(data)

                videoList.add(VideoItem(id, uri, name, duration, size, dateModified, videoFolderPath, folderName, subtitleUri))
            }
        }

        emit(videoList)
    }.flowOn(Dispatchers.IO)
    private fun findSubtitleForVideo(videoPath: String): android.net.Uri? {
        try {
            val videoFile = java.io.File(videoPath)
            val parentFile = videoFile.parentFile ?: return null
            val baseName = videoFile.nameWithoutExtension
            
            val extensions = arrayOf("srt", "ass", "ssa", "vtt")
            for (ext in extensions) {
                val subtitleFile = java.io.File(parentFile, "$baseName.$ext")
                if (subtitleFile.exists()) {
                    return android.net.Uri.fromFile(subtitleFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
