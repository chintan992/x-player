package com.chintan992.xplayer

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.chintan992.xplayer.data.local.FileScanner
import com.chintan992.xplayer.data.local.SubtitleFinder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LocalMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileScanner: FileScanner,
    private val subtitleFinder: SubtitleFinder
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
                val subtitleUri = subtitleFinder.findSubtitleForVideo(data)

                videoList.add(VideoItem(id, uri, name, duration, size, dateModified, folderPath, folderName, subtitleUri))
            }
        }
        
        // Emit visible videos immediately
        emit(videoList.toList())
        
        if (includeHidden) {
            val hiddenVideos = getManualHiddenVideos()
            if (hiddenVideos.isNotEmpty()) {
                val combinedList = videoList + hiddenVideos
                emit(combinedList)
            }
        }
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
                val subtitleUri = subtitleFinder.findSubtitleForVideo(data)

                val video = VideoItem(id, uri, name, duration, size, dateModified, folderPath, folderName, subtitleUri)
                folderMap.getOrPut(folderPath) { mutableListOf() }.add(video)
            }
        }
        
        // Helper to convert map to sorted folder list
        fun mapToFolders(): List<VideoFolder> {
            return folderMap.map { (path, videos) ->
                VideoFolder(
                    path = path,
                    name = videos.firstOrNull()?.folderName ?: path.substringAfterLast("/"),
                    videoCount = videos.size,
                    totalSize = videos.sumOf { it.size },
                    thumbnailUri = videos.firstOrNull()?.uri
                )
            }.sortedBy { it.name.lowercase() }
        }

        // Emit visible folders immediately
        emit(mapToFolders())
        
        // Manual Scan for Hidden
        if (includeHidden) {
            val hiddenVideos = getManualHiddenVideos()
            if (hiddenVideos.isNotEmpty()) {
                for (video in hiddenVideos) {
                    folderMap.getOrPut(video.folderPath) { mutableListOf() }.add(video)
                }
                // Emit updated folders
                emit(mapToFolders())
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getVideosByFolder(folderPath: String, includeHidden: Boolean = false): Flow<List<VideoItem>> = flow {
        val videoList = mutableListOf<VideoItem>()
        
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
                val subtitleUri = subtitleFinder.findSubtitleForVideo(data)

                videoList.add(VideoItem(id, uri, name, duration, size, dateModified, videoFolderPath, folderName, subtitleUri))
            }
        }
        
        // Emit visible videos immediately
        emit(videoList.toList())
        
        if (includeHidden) {
            try {
                val dir = java.io.File(folderPath)
                if (dir.exists() && dir.isDirectory) {
                    val manualVideos = fileScanner.scanDirectoryForVideos(dir, recursive = false)
                    val existingPaths = videoList.map { it.name }.toSet()
                    
                    val newVideos = mutableListOf<VideoItem>()
                    for (v in manualVideos) {
                         if (!existingPaths.contains(v.name)) {
                             newVideos.add(v)
                         }
                    }
                    
                    if (newVideos.isNotEmpty()) {
                        emit(videoList + newVideos)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getManualHiddenVideos(): List<VideoItem> {
        val hiddenVideos = mutableListOf<VideoItem>()
        try {
            val root = android.os.Environment.getExternalStorageDirectory()
            val dirs = root.listFiles { file -> 
                file.isDirectory && file.name.startsWith(".") && !file.name.equals(".") && !file.name.equals("..") 
            } ?: emptyArray()

            for (dir in dirs) {
                hiddenVideos.addAll(fileScanner.scanDirectoryForVideos(dir, recursive = true))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return hiddenVideos
    }
    
    // Extracted helper methods removed - Keeping this comment as anchor

    // File Operations
    suspend fun deleteVideo(video: VideoItem): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val file = java.io.File(video.folderPath, video.name)
            if (file.exists() && file.delete()) {
                // If file delete worked, try to clean up MediaStore
                try {
                    context.contentResolver.delete(video.uri, null, null)
                } catch (e: Exception) {
                    // Ignore, maybe not in MediaStore or already gone
                }
                return@withContext true
            }
            // Fallback to MediaStore delete
            try {
                if (context.contentResolver.delete(video.uri, null, null) > 0) return@withContext true
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteFolder(folder: VideoFolder): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val dir = java.io.File(folder.path)
            if (dir.exists() && dir.isDirectory) {
                // Recursive delete
                return@withContext dir.deleteRecursively()
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun renameVideo(video: VideoItem, newName: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val sourceFile = java.io.File(video.folderPath, video.name)
            val destFile = java.io.File(video.folderPath, newName)
            
            if (sourceFile.renameTo(destFile)) {
                 // Scan new file to MediaStore
                android.media.MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
                // Remove old from MediaStore (optional, scanner might handle update but delete ensures no ghost)
                // context.contentResolver.delete(video.uri, null, null) 
                return@withContext true
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun renameFolder(folder: VideoFolder, newName: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
         try {
            val sourceDir = java.io.File(folder.path)
            val parent = sourceDir.parentFile ?: return@withContext false
            val destDir = java.io.File(parent, newName)
            
            if (sourceDir.renameTo(destDir)) {
                 // Rescan is tricky for folders, might need to scan all files inside. 
                 // For now just return true and let app refresh
                return@withContext true
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun moveVideo(video: VideoItem, targetFolderPath: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
             val sourceFile = java.io.File(video.folderPath, video.name)
             val destFile = java.io.File(targetFolderPath, video.name)
             
             if (sourceFile.renameTo(destFile)) {
                 android.media.MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
                 return@withContext true
             }
             // Cross-filesystem move fallback (copy + delete)
             if (copyVideo(video, targetFolderPath)) {
                 return@withContext deleteVideo(video)
             }
             false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun copyVideo(video: VideoItem, targetFolderPath: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val destFile = java.io.File(targetFolderPath, video.name)
            
            // Check if destination exists and handle collision
            if (destFile.exists()) {
                // For now, if same path return true, if different file with same name return false or overwrite? 
                // Let's assume overwrite for this implementation but maybe we should fail safely.
                if (destFile.absolutePath == video.folderPath + "/" + video.name) return@withContext true
            }

            // Use ContentResolver to open input stream - reliable for MediaStore items
            val inputStream = context.contentResolver.openInputStream(video.uri) ?: return@withContext false
            
            inputStream.use { input ->
                java.io.FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8 * 1024) // 8KB buffer
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                    // Force write to disk
                    output.fd.sync()
                }
            }

            // Verify size matches (simple integrity check)
            // Note: video.size might not be perfectly reliable if MediaStore is stale, but it's a good first check.
            // Better to check source File length if accessible, but we are using Uri.
            // Let's rely on the fact we copied stream to completion without exception.
            // However, we can try to get length from PFD if possible.
            try {
                 context.contentResolver.openFileDescriptor(video.uri, "r")?.use { pfd ->
                     if (pfd.statSize != destFile.length()) {
                         destFile.delete()
                         return@withContext false
                     }
                 }
            } catch (e: Exception) {
                // If we can't check size, assume it worked if no IO exception occurred above
            }

            android.media.MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
