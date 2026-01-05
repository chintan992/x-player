package com.chintan992.xplayer.datasource

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import java.io.IOException
import java.io.InputStream

@OptIn(UnstableApi::class)
class WebDavDataSource : BaseDataSource(true) {

    private var dataSpec: DataSpec? = null
    private var inputStream: InputStream? = null
    private var sardine: Sardine? = null
    private var bytesRemaining: Long = 0
    private var opened = false

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        transferInitializing(dataSpec)

        try {
            val uri = dataSpec.uri
            // WebDav often uses http/https but we use a custom scheme or just handle it if passed explicitly.
            // Assuming scheme webdav:// or webdavs:// which we map to http/https
            
            val host = uri.host ?: throw IOException("Invalid WebDAV URI: Host is missing")
            val userInfo = uri.userInfo
            val username = userInfo?.substringBefore(':') ?: ""
            val password = userInfo?.substringAfter(':', "") ?: ""

            // Reconstruct the HTTP URL
            // If scheme is webdav -> http, webdavs -> https
            val httpScheme = if (uri.scheme == "webdavs") "https" else "http"
            // Use standard port if not specified: 80 for http, 443 for https
            val port = if (uri.port > 0) ":${uri.port}" else ""
            
            // Build the URL, removing user info from authority
            val newAuthority = host + (if (uri.port > 0) ":${uri.port}" else "")
            val buildUri = uri.buildUpon().scheme(httpScheme).encodedAuthority(newAuthority).build()
            val url = buildUri.toString()

            sardine = OkHttpSardine()
            if (username.isNotEmpty()) {
                sardine!!.setCredentials(username, password)
            }

            // Get Content-Length if possible using HEAD (or just list resources)
            // Sardine usually offers list() or getResource()
            // However, verify if get returns a stream first can be safer for just playing.
            // For seeking, we can pass "Range" header if supported, but Sardine interface is high level.
            // OkHttpSardine uses OkHttp, maybe we can use custom headers?
            // Sardine.get(url, headerMap) exists?
            // Checking the library, standard .get(url) returns InputStream.
            // Sardine doesn't easily expose range requests in the basic interface, 
            // but we can try to assume it reads from start or we skip.
            
            // NOTE: Efficient seeking on WebDAV with sardine-android might be tricky without Range header support in the get() call.
            // We will do a basic implementation. If needed we can extend or use OkHttpDataSource directly with WebDAV verbs if required.
            // Actually, WebDAV for reading is just HTTP GET. 
            // So we can actually use OkHttpDataSource with credentials!
            // But let's stick to Sardine for now as requested, or switch if better. 
            // Doing it this way for "WebDAV" specific handling like listing. 
            // But for streaming, it IS just HTTP.
            // Let's implement reading using Sardine.get().
            
            // Check file existence/size
            if (sardine!!.exists(url)) {
                val resources = sardine!!.list(url)
                if (resources.isNotEmpty()) {
                    // StartAppSDK/sardine-android might return the file itself as first item or list
                    // Usually list(fileUrl) returns list of 1.
                     val res = resources[0]
                     val fileLength = res.contentLength
                     
                     if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                        bytesRemaining = dataSpec.length
                    } else {
                        bytesRemaining = fileLength - dataSpec.position
                    }
                }
            }

            // Ideally we want to send "Range: bytes=$position-" header.
            // Sardine's get(url) does not take headers in some versions.
            // If avoiding headers, we must skip.
            // Let's check imports. com.thegrizzlylabs.sardineandroid.Sardine
            
            inputStream = sardine!!.get(url)
            
            if (dataSpec.position > 0) {
                inputStream?.skip(dataSpec.position)
            }

            if (bytesRemaining <= 0) {
                 // Fallback if we couldn't get length
                 bytesRemaining = C.LENGTH_UNSET.toLong()
            }
            
            opened = true
            transferStarted(dataSpec)
            return bytesRemaining
        } catch (e: Exception) {
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
        if (opened) {
            opened = false
            transferEnded()
        }
    }
}
