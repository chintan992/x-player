package com.chintan992.xplayer

import android.content.IntentSender

/**
 * Sealed class representing the result of file operations.
 * Supports success, permission requests (for scoped storage), and errors.
 */
sealed class FileOperationResult {
    /**
     * Operation completed successfully
     * @param count Number of items affected
     */
    data class Success(val count: Int) : FileOperationResult()
    
    /**
     * User needs to grant permission for this operation (Android 10+ scoped storage)
     * @param intentSender The intent sender to launch for user consent
     */
    data class NeedsPermission(val intentSender: IntentSender) : FileOperationResult()
    
    /**
     * Operation failed with an error
     * @param message Human-readable error message
     */
    data class Error(val message: String) : FileOperationResult()
}
