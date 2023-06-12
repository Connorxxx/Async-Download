package com.connor.asyncdownload.model.repo

import android.content.Context
import com.connor.asyncdownload.model.data.DownloadData
import com.connor.asyncdownload.model.data.KtorDownload
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.type.P
import com.connor.asyncdownload.utils.formatSize
import com.connor.asyncdownload.utils.getFileNameFromUrl
import com.connor.asyncdownload.utils.onStreaming
import dagger.hilt.android.qualifiers.ApplicationContext
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

@Singleton
class HttpRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val client: HttpClient
) {
    suspend fun <T: KtorDownload> downloadFile(download: T) = channelFlow {
        val name = download.url.getFileNameFromUrl() ?: "error"
        val file = File(ctx.cacheDir, name)
        var exitsBytes = download.downBytes
        val rangeHeader = "bytes=${download.downBytes}-"
        var lastUpdateTime = System.currentTimeMillis()
        send(DownloadType.Started(name,download))
        client.prepareGet(download.url) {
            header(HttpHeaders.Range, rangeHeader)
            onDownload { _, length ->
                val totalLength = exitsBytes + length
                val progress = (download.downBytes * 100f / totalLength).roundToInt().toString()
                val currentTime = System.currentTimeMillis()
                val timeSinceLastUpdate = currentTime - lastUpdateTime
                if (timeSinceLastUpdate >= 500) {
                    val p = P(progress, download.downBytes.formatSize(), totalLength.formatSize())
                    if (download.downBytes == 0L) send(DownloadType.Waiting(download))
                    else send(DownloadType.Progress(p, download))
                    lastUpdateTime = currentTime
                }
            }
        }.execute {
            if (it.status != HttpStatusCode.PartialContent) {
                file.delete()
                download.downBytes = 0
                exitsBytes = 0
            }
            it.bodyAsChannel().onStreaming(file, download) { s ->
                send(DownloadType.Speed(s, download))
            }
        }
        send(DownloadType.Finished(file, download))
    }.catch { error ->
        error.printStackTrace()
        emit(DownloadType.Failed(error, download))
    }.flowOn(Dispatchers.IO)
}