package com.connor.asyncdownload.model

import android.content.Context
import com.connor.asyncdownload.BuildConfig
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.utils.getFileNameFromUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class Repository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val client: HttpClient
) {

    suspend fun downloadFile(link: Link) = channelFlow {
        val httpResponse = client.get(link.url) {
            onDownload { bytesSentTotal, contentLength ->
                val progress = (bytesSentTotal * 100f / contentLength).roundToInt().toString()
                send(DownloadType.Progress(progress, link))
            }
        }.bodyAsChannel()
        val file = File(ctx.filesDir, link.url.getFileNameFromUrl() ?: "error")
        httpResponse.copyAndClose(file.writeChannel())
        send(DownloadType.Finished(file, link))
    }.onStart {
        emit(DownloadType.Started(link))
    }.catch { error ->
        error.printStackTrace()
        emit(DownloadType.Failed(error, link))
    }.flowOn(Dispatchers.IO)

    private fun deleteDirectoryContents(directory: File) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteDirectoryContents(file)
            }
            if (file.length() > 10 * 1024 * 1024) file.delete()
        }
    }
}