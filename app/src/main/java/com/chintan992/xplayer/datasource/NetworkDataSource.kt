package com.chintan992.xplayer.datasource

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener

@OptIn(UnstableApi::class)
class NetworkDataSource(
    private val context: Context,
    private val httpDataSourceFactory: DataSource.Factory
) : DataSource {

    private val defaultDataSource: DataSource = DefaultDataSource(context, httpDataSourceFactory.createDataSource())
    private var smbDataSource: SmbDataSource? = null
    private var ftpDataSource: FtpDataSource? = null
    private var webDavDataSource: WebDavDataSource? = null
    private var currentDataSource: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        defaultDataSource.addTransferListener(transferListener)
        getSmbDataSource().addTransferListener(transferListener)
        getFtpDataSource().addTransferListener(transferListener)
        getWebDavDataSource().addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val scheme = dataSpec.uri.scheme
        currentDataSource = when (scheme) {
            "smb" -> getSmbDataSource()
            "ftp" -> getFtpDataSource()
            "webdav", "webdavs" -> getWebDavDataSource()
            else -> defaultDataSource
        }
        return currentDataSource!!.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        return currentDataSource?.read(buffer, offset, readLength) ?: 0
    }

    override fun getUri(): Uri? {
        return currentDataSource?.uri
    }

    override fun close() {
        currentDataSource?.close()
        currentDataSource = null
    }

    private fun getSmbDataSource(): SmbDataSource {
        if (smbDataSource == null) {
            smbDataSource = SmbDataSource()
        }
        return smbDataSource!!
    }

    private fun getFtpDataSource(): FtpDataSource {
        if (ftpDataSource == null) {
            ftpDataSource = FtpDataSource()
        }
        return ftpDataSource!!
    }

    private fun getWebDavDataSource(): WebDavDataSource {
        if (webDavDataSource == null) {
            webDavDataSource = WebDavDataSource()
        }
        return webDavDataSource!!
    }

    class Factory(
        private val context: Context,
        private val httpDataSourceFactory: DataSource.Factory
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return NetworkDataSource(context, httpDataSourceFactory)
        }
    }
}
