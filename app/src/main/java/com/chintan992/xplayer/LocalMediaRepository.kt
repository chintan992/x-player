package com.chintan992.xplayer

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.chintan992.xplayer.data.local.FileScanner
import com.chintan992.xplayer.data.local.SubtitleFinder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
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
    
    // File Operations
    
    /**
     * Delete a single video with modern scoped storage handling.
     * Returns FileOperationResult for proper error handling.
     */
    suspend fun deleteVideoModern(video: VideoItem): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            // Try ContentResolver delete first (works for app-owned files)
            val deleted = context.contentResolver.delete(video.uri, null, null)
            if (deleted > 0) {
                return@withContext FileOperationResult.Success(1)
            }
            
            // Fallback: Try direct file delete (for files with write access)
            val file = java.io.File(video.folderPath, video.name)
            if (file.exists() && file.canWrite() && file.delete()) {
                return@withContext FileOperationResult.Success(1)
            }
            
            FileOperationResult.Error("Could not delete file. Check permissions.")
        } catch (e: SecurityException) {
            // Handle RecoverableSecurityException for Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                FileOperationResult.NeedsPermission(e.userAction.actionIntent.intentSender)
            } else {
                FileOperationResult.Error("Permission denied: ${e.message}")
            }
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Unknown error during delete")
        }
    }
    
    /**
     * Create a delete request for multiple videos (Android R+ only).
     * Returns an IntentSender for user consent dialog.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createBulkDeleteRequest(uris: List<Uri>): android.app.PendingIntent {
        return MediaStore.createDeleteRequest(context.contentResolver, uris)
    }
    
    /**
     * Delete multiple videos with proper Android version handling.
     */
    suspend fun deleteVideosModern(videos: List<VideoItem>): FileOperationResult = withContext(Dispatchers.IO) {
        if (videos.isEmpty()) return@withContext FileOperationResult.Success(0)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: Use createDeleteRequest for system consent dialog
            try {
                val uris = videos.map { it.uri }
                val pendingIntent = createBulkDeleteRequest(uris)
                return@withContext FileOperationResult.NeedsPermission(pendingIntent.intentSender)
            } catch (e: Exception) {
                return@withContext FileOperationResult.Error("Failed to create delete request: ${e.message}")
            }
        } else {
            // Android 10 and below: Delete one by one
            var successCount = 0
            var lastError: String? = null
            
            for (video in videos) {
                when (val result = deleteVideoModern(video)) {
                    is FileOperationResult.Success -> successCount += result.count
                    is FileOperationResult.NeedsPermission -> {
                        // Return immediately for permission request
                        return@withContext result
                    }
                    is FileOperationResult.Error -> lastError = result.message
                }
            }
            
            if (successCount > 0) {
                FileOperationResult.Success(successCount)
            } else {
                FileOperationResult.Error(lastError ?: "Failed to delete files")
            }
        }
    }

    // Legacy delete method for backwards compatibility
    suspend fun deleteVideo(video: VideoItem): Boolean = withContext(Dispatchers.IO) {
        when (val result = deleteVideoModern(video)) {
            is FileOperationResult.Success -> true
            else -> false
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
    
    /**
     * Move a video to a target folder using modern scoped storage practices.
     * Returns FileOperationResult for proper error handling.
     */
    suspend fun moveVideoModern(video: VideoItem, targetFolderPath: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val sourceFile = java.io.File(video.folderPath, video.name)
            val destFile = java.io.File(targetFolderPath, video.name)
            
            // Check if already in target folder
            if (video.folderPath == targetFolderPath) {
                return@withContext FileOperationResult.Success(1)
            }
            
            // Try direct rename first (fast, atomic - works on same filesystem if we have write access)
            if (sourceFile.exists() && sourceFile.canWrite()) {
                if (sourceFile.renameTo(destFile)) {
                    android.media.MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
                    // Update MediaStore for old location
                    try {
                        context.contentResolver.delete(video.uri, null, null)
                    } catch (e: Exception) {
                        // Ignore - MediaScanner will handle it
                    }
                    return@withContext FileOperationResult.Success(1)
                }
            }
            
            // Fallback: Copy to destination first
            if (!copyVideo(video, targetFolderPath)) {
                return@withContext FileOperationResult.Error("Failed to copy file to destination")
            }
            
            // Now delete the original using modern API
            when (val deleteResult = deleteVideoModern(video)) {
                is FileOperationResult.Success -> {
                    FileOperationResult.Success(1)
                }
                is FileOperationResult.NeedsPermission -> {
                    // User needs to grant permission to delete original
                    // The copy was successful, so we return the permission request
                    // After permission is granted, the original will be deleted
                    deleteResult
                }
                is FileOperationResult.Error -> {
                    // Copy succeeded but delete failed
                    // This leaves a duplicate, but move is partially complete
                    FileOperationResult.Error("Copied to destination but couldn't remove original: ${deleteResult.message}")
                }
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is android.app.RecoverableSecurityException) {
                FileOperationResult.NeedsPermission(e.userAction.actionIntent.intentSender)
            } else {
                FileOperationResult.Error("Permission denied: ${e.message}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FileOperationResult.Error(e.message ?: "Failed to move file")
        }
    }

    // Legacy move method for backwards compatibility
    suspend fun moveVideo(video: VideoItem, targetFolderPath: String): Boolean = withContext(Dispatchers.IO) {
        when (val result = moveVideoModern(video, targetFolderPath)) {
            is FileOperationResult.Success -> true
            else -> false
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
