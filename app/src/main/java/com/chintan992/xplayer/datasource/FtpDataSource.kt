package com.chintan992.xplayer.datasource

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.IOException
import java.io.InputStream

@OptIn(UnstableApi::class)
class FtpDataSource : BaseDataSource(true) {

    private var dataSpec: DataSpec? = null
    private var ftpClient: FTPClient? = null
    private var inputStream: InputStream? = null
    private var bytesRemaining: Long = 0
    private var opened = false

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        transferInitializing(dataSpec)

        try {
            val uri = dataSpec.uri
            val host = uri.host ?: throw IOException("Invalid FTP URI: Host is missing")
            val port = if (uri.port > 0) uri.port else 21
            val userInfo = uri.userInfo
            val username = userInfo?.substringBefore(':') ?: "anonymous"
            val password = userInfo?.substringAfter(':', "") ?: ""

            ftpClient = FTPClient()
            ftpClient!!.connect(host, port)
            
            if (!ftpClient!!.login(username, password)) {
                throw IOException("FTP login failed")
            }

            ftpClient!!.enterLocalPassiveMode()
            ftpClient!!.setFileType(FTP.BINARY_FILE_TYPE)
            
            val path = uri.path
            if (path.isNullOrEmpty()) throw IOException("Invalid FTP URI: Path is missing")

            // Determine file size if possible for bytesRemaining
            // Not strictly required for streaming but good for buffering
            val fileList = ftpClient!!.listFiles(path)
            val fileLength = if (fileList.isNotEmpty()) fileList[0].size else C.LENGTH_UNSET.toLong()

            ftpClient!!.restartOffset = dataSpec.position
            inputStream = ftpClient!!.retrieveFileStream(path)
                ?: throw IOException("Could not open FTP file stream for $path")

            if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                bytesRemaining = dataSpec.length
            } else if (fileLength != C.LENGTH_UNSET.toLong()) {
                bytesRemaining = fileLength - dataSpec.position
            } else {
                bytesRemaining = C.LENGTH_UNSET.toLong()
            }

            opened = true
            transferStarted(dataSpec)
            return bytesRemaining
        } catch (e: Exception) {
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
        if (inputStream != null) {
            try {
                inputStream!!.close()
            } catch (e: IOException) {
                // Ignore
            }
            inputStream = null
        }
        
        // completePendingCommand is necessary if we want to reuse the client,
        // but here we are closing it.
        
        closeResources()
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    private fun closeResources() {
        if (ftpClient != null && ftpClient!!.isConnected) {
            try {
                ftpClient!!.disconnect()
            } catch (e: IOException) {
                // Ignore
            }
        }
        ftpClient = null
    }
}
