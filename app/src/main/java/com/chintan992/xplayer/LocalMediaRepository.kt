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

    fun getVideos(includeHidden: Boolean = false): Flow<List<VideoItem>> = flow {
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
                
                val folderPath = data.substringBeforeLast("/", "")
                val subtitleUri = findSubtitleForVideo(data)

                videoList.add(VideoItem(id, uri, name, duration, size, dateModified, folderPath, folderName, subtitleUri))
            }
        }
        
        if (includeHidden) {
            videoList.addAll(getManualHiddenVideos())
        }

        emit(videoList)
    }.flowOn(Dispatchers.IO)

    fun getVideoFolders(includeHidden: Boolean = false): Flow<List<VideoFolder>> = flow {
        val folderMap = mutableMapOf<String, MutableList<VideoItem>>()
        
        // MediaStore Videos
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
        
        // Manual Scan for Hidden
        if (includeHidden) {
            val hiddenVideos = getManualHiddenVideos()
            for (video in hiddenVideos) {
                folderMap.getOrPut(video.folderPath) { mutableListOf() }.add(video)
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

    fun getVideosByFolder(folderPath: String, includeHidden: Boolean = false): Flow<List<VideoItem>> = flow {
        val videoList = mutableListOf<VideoItem>()
        
        // If it's a hidden folder, MediaStore likely returns nothing, but we check anyway
        val selection = "$mimeTypeSelection AND ${MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = validMimeTypes + "$folderPath/%"

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            ),
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
        
        if (includeHidden) {
            // Check if this specific folder has manual videos
            // Optimization: if we already scan everything in getManualHiddenVideos, we can just filter
            // But doing a full scan for one folder is inefficient?
            // "getManualHiddenVideos" scans ROOT hidden folders.
            // If folderPath is inside a hidden folder, or IS a hidden folder, we need to scan IT.
            
            // If folderPath starts with ".", it's hidden (relative to storage root usually, or just name)
            // We'll just scan the directory using File API
            try {
                val dir = java.io.File(folderPath)
                if (dir.exists() && dir.isDirectory) {
                    val manualVideos = scanDirectoryForVideos(dir, recursive = false) // user is asking for this folder content
                    // Merge, avoiding duplicates (by path/URI)
                    val existingPaths = videoList.map { 
                        // URI -> Path resolution is hard without context, but we can assume ID based uniqueness or Data path
                        // For MediaStore items, we don't easily have 'Data' unless we kept it.
                        // We do have 'folderPath' but not file path in VideoItem
                        // Let's rely on name? VideoItem has `name`.
                        it.name
                    }.toSet()
                    
                    for (v in manualVideos) {
                         if (!existingPaths.contains(v.name)) {
                             videoList.add(v)
                         }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        emit(videoList)
    }.flowOn(Dispatchers.IO)

    private fun getManualHiddenVideos(): List<VideoItem> {
        val hiddenVideos = mutableListOf<VideoItem>()
        try {
            val root = android.os.Environment.getExternalStorageDirectory()
            val dirs = root.listFiles { file -> 
                file.isDirectory && file.name.startsWith(".") && !file.name.equals(".") && !file.name.equals("..") 
            } ?: emptyArray()

            for (dir in dirs) {
                hiddenVideos.addAll(scanDirectoryForVideos(dir, recursive = true))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return hiddenVideos
    }
    
    private fun scanDirectoryForVideos(dir: java.io.File, recursive: Boolean): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val files = dir.listFiles() ?: return emptyList()
        
        for (file in files) {
            if (file.isDirectory) {
                if (recursive) {
                    videos.addAll(scanDirectoryForVideos(file, true))
                }
            } else {
                if (isValidVideoFile(file.name)) {
                    val uri = android.net.Uri.fromFile(file)
                    val name = file.name
                    val parent = file.parentFile
                    val folderName = parent?.name ?: "Unknown"
                    val folderPath = parent?.absolutePath ?: ""
                    val size = file.length()
                    val modified = file.lastModified() / 1000
                    
                    // Duration not easily available without extracting, set to 0
                    videos.add(VideoItem(
                        id = file.hashCode().toLong(), // Synthetic ID
                        uri = uri,
                        name = name,
                        duration = 0L,
                        size = size,
                        dateModified = modified,
                        folderPath = folderPath,
                        folderName = folderName,
                        subtitleUri = findSubtitleForVideo(file.absolutePath)
                    ))
                }
            }
        }
        return videos
    }
    
    private fun isValidVideoFile(name: String): Boolean {
        val extensions = arrayOf(".mp4", ".mkv", ".webm", ".avi", ".mov", ".3gp")
        return extensions.any { name.endsWith(it, ignoreCase = true) }
    }

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
