package com.chintan992.xplayer.datasource

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
class SmbDataSource : BaseDataSource(true) {

    private var dataSpec: DataSpec? = null
    private var inputStream: InputStream? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var diskShare: DiskShare? = null
    private var smbFile: File? = null
    private var bytesRemaining: Long = 0
    private var opened = false

    companion object {
        // Cache connections to reuse them, avoiding repeated handshakes
        private val client = SMBClient()
        // Simple cache: Host -> Connection. Note: This is a basic implementation.
        // In a real app, you might want connection cooling, etc.
        // For now, we will create new connections per viewing session to be safe, 
        // or we can keep a static map if we want to support seeking efficiently 
        // without reconnecting every time if the DataSource is recreated.
        // Given existing structure, we will connect on open() and close on close().
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        transferInitializing(dataSpec)

        try {
            val uri = dataSpec.uri
            val host = uri.host ?: throw IOException("Invalid SMB URI: Host is missing")
            val port = if (uri.port > 0) uri.port else 445
            
            // Expected URI format: smb://user:password@host/share/path/to/file
            // or smb://host/share/path/to/file (anonymous)
            
            val userInfo = uri.userInfo
            val username = userInfo?.substringBefore(':') ?: ""
            val password = userInfo?.substringAfter(':', "") ?: ""
            val domain = "" // Domain support could be added via query param if needed

            connection = client.connect(host, port)
            val authContext = if (username.isNotEmpty()) {
                AuthenticationContext(username, password.toCharArray(), domain)
            } else {
                AuthenticationContext.guest()
            }
            
            session = connection?.authenticate(authContext)
            
            // Path parsing: /share/path/to/file
            // The first segment is the share name.
            val pathSegments = uri.pathSegments
            if (pathSegments.isEmpty()) throw IOException("Invalid SMB URI: Share name missing")
            
            val shareName = pathSegments[0]
            val filePath = pathSegments.drop(1).joinToString("\\") // SMB uses backslashes
            
            diskShare = session?.connectShare(shareName) as? DiskShare
                ?: throw IOException("Could not connect to share: $shareName")

            if (!diskShare!!.fileExists(filePath)) {
                throw IOException("File not found: $filePath")
            }

            smbFile = diskShare!!.openFile(
                filePath,
                setOf(AccessMask.FILE_READ_DATA),
                null,
                setOf(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            
            // Handle position
            if (dataSpec.position > 0) {
                 // Skip input stream is inefficient for SMB, better to seek if possible or just read from offset
                 // SMBJ's InputStream supports skip, but let's see.
                 // Actually smbFile.getInputStream() returns an InputStream. 
                 // We can assume it supports skip, or we might need to handle it.
            }

            inputStream = smbFile!!.inputStream
            
            if (dataSpec.position > 0) {
                inputStream?.skip(dataSpec.position)
            }

            val fileLength = diskShare!!.getFileInformation(filePath).standardInformation.endOfFile
            
            if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                bytesRemaining = dataSpec.length
            } else {
                bytesRemaining = fileLength - dataSpec.position
            }
            
            opened = true
            transferStarted(dataSpec)
            return bytesRemaining
        } catch (e: Exception) {
            // cleanup if failed
            closeResources()
            throw IOException(e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (readLength == 0) {
            return 0
        }
        if (bytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }

        val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            readLength
        } else {
            readLength.toLong().coerceAtMost(bytesRemaining).toInt()
        }

        val bytesRead: Int
        try {
            bytesRead = inputStream!!.read(buffer, offset, bytesToRead)
        } catch (e: IOException) {
            throw IOException(e)
        }

        if (bytesRead == -1) {
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                // We expected to read more data but hit EOF.
                throw IOException("End of stream reached prematurely")
            }
            return C.RESULT_END_OF_INPUT
        }

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? {
        return dataSpec?.uri
    }

    override fun close() {
        try {
            if (inputStream != null) {
                try {
                    inputStream!!.close()
                } catch (e: IOException) {
                    // Log or ignore
                }
            }
        } finally {
            inputStream = null
            closeResources()
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    private fun closeResources() {
        try {
            smbFile?.close()
        } catch (e: Exception) {}
        smbFile = null

        try {
            diskShare?.close()
        } catch (e: Exception) {}
        diskShare = null

        try {
            session?.close()
        } catch (e: Exception) {}
        session = null

        try {
            connection?.close()
        } catch (e: Exception) {}
        connection = null
    }
}
