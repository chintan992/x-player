package com.chintan992.xplayer.cast

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.IOException

class LocalVideoServer(
    private val context: Context,
    private val videoUri: Uri
) : NanoHTTPD(8080) {

    override fun serve(session: IHTTPSession): Response {
        val mimeType = "video/mp4"
        val rangeHeader = session.headers["range"]

        return try {
            val fileLength: Long = getFileLength(videoUri)
            
            if (rangeHeader != null) {
                val rangeValue = rangeHeader.substring("bytes=".length)
                val start: Long
                val end: Long
                
                if (rangeValue.startsWith("-")) {
                    end = fileLength - 1
                    start = fileLength - rangeValue.substring(1).toLong()
                } else {
                    val parts = rangeValue.split("-")
                    start = parts[0].toLong()
                    end = if (parts.size > 1 && parts[1].isNotEmpty()) {
                        parts[1].toLong()
                    } else {
                        fileLength - 1
                    }
                }

                if (start >= fileLength) {
                    return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "")
                        .apply { addHeader("Content-Range", "bytes */$fileLength") }
                }

                val contentLength = end - start + 1
                val inputStream = openStream()
                inputStream?.skip(start)

                val response = newFixedLengthResponse(
                    Response.Status.PARTIAL_CONTENT,
                    mimeType,
                    inputStream,
                    contentLength
                )
                response.addHeader("Content-Range", "bytes $start-$end/$fileLength")
                response.addHeader("Accept-Ranges", "bytes")
                response.addHeader("Content-Length", contentLength.toString())
                return response

            } else {
                 val inputStream = openStream()
                 val response = newFixedLengthResponse(
                    Response.Status.OK,
                    mimeType,
                    inputStream,
                    fileLength
                )
                response.addHeader("Accept-Ranges", "bytes")
                response.addHeader("Content-Length", fileLength.toString())
                return response
            }
        } catch (e: Exception) {
            e.printStackTrace()
             newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.message)
        }
    }

    private fun openStream(): InputStream? {
        return if (videoUri.scheme == "content") {
            context.contentResolver.openInputStream(videoUri)
        } else {
            val path = videoUri.path
            val file = File(path ?: "")
            if (file.exists()) FileInputStream(file) else null
        }
    }

    private fun getFileLength(uri: Uri): Long {
         if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        return cursor.getLong(sizeIndex)
                    }
                }
            }
             // Fallback if query fails, though inaccurate for streams usually
             try {
                return context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0
             } catch(e: IOException) {
                 return 0
             }
         } else {
             val path = uri.path
             val file = File(path ?: "")
             return if (file.exists()) file.length() else 0
         }
    }
}
