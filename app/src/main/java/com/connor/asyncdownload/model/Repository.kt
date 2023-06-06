package com.connor.asyncdownload.model

import android.content.Context
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.utils.downloadedBytes
import com.connor.asyncdownload.utils.getFileNameFromUrl
import com.connor.asyncdownload.utils.logCat
import com.connor.asyncdownload.utils.onStreaming
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
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
        val file = File(ctx.filesDir, link.url.getFileNameFromUrl() ?: "error")
        client.prepareGet(link.url) {
            onDownload { _, contentLength ->
                val progress = (downloadedBytes * 100f / contentLength).roundToInt().toString()
                if (progress == "0") send(DownloadType.Waiting(link))
                else send(DownloadType.Progress(progress, link))
            }
        }.execute {
            it.bodyAsChannel().onStreaming(file, it.contentLength()) { v ->
                send(DownloadType.Speed(v, link))
            }
        }
        send(DownloadType.Finished(file, link))
    }.onStart {
        emit(DownloadType.Started(link))
    }.onCompletion { emit(DownloadType.Canceled(link)) }
        .catch { error ->
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