package com.connor.asyncdownload.model

import android.content.Context
import com.connor.asyncdownload.model.data.KtorDownload
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.utils.formatSize
import com.connor.asyncdownload.utils.getFileNameFromUrl
import com.connor.asyncdownload.utils.logCat
import com.connor.asyncdownload.utils.onStreaming
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@ViewModelScoped
class Repository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val client: HttpClient
) {

    suspend fun downloadFile(download: KtorDownload) = channelFlow {
        val file = File(ctx.filesDir, download.url.getFileNameFromUrl() ?: "error")
        var exitsBytes = download.downBytes
        val rangeHeader = "bytes=${download.downBytes}-"
        var lastUpdateTime = System.currentTimeMillis()
//        if (file.exists()) {
//            send(DownloadType.FileExists(download))
//            return@channelFlow
//        }
        send(DownloadType.Started(download))
        client.prepareGet(download.url) {
            header(HttpHeaders.Range, rangeHeader)
            onDownload { _, length ->
                val totalLength = exitsBytes + length
                val progress = (download.downBytes * 100f / totalLength).roundToInt().toString()
                val currentTime = System.currentTimeMillis()
                val timeSinceLastUpdate = currentTime - lastUpdateTime

                if (timeSinceLastUpdate >= 500) {
                    send(
                        DownloadType.Size(
                            download.downBytes.formatSize(),
                            totalLength.formatSize(),
                            download
                        )
                    )
                    if (download.downBytes == 0L) send(DownloadType.Waiting(download))
                    else send(DownloadType.Progress(progress, download))
                    lastUpdateTime = currentTime
                }
            }
        }.execute {
            it.status.logCat()
            if (it.status != HttpStatusCode.PartialContent) {
                file.delete()
                download.downBytes = 0
                exitsBytes = 0
            }
            it.bodyAsChannel().onStreaming(file, download) { v ->
                send(DownloadType.Speed(v, download))
            }
        }
        send(DownloadType.Finished(file, download))
    }.catch { error ->
        error.printStackTrace()
        emit(DownloadType.Failed(error, download))
    }.flowOn(Dispatchers.IO)
}