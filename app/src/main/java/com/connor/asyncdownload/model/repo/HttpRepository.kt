package com.connor.asyncdownload.model.repo

import android.content.Context
import com.connor.asyncdownload.model.data.KtorDownload
import com.connor.asyncdownload.type.P
import com.connor.asyncdownload.utils.*
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
    fun <T: KtorDownload> downloadFile(download: T) = channelFlow {
        val name = download.url.getFileNameFromUrl() ?: "error"
        val file = File(ctx.cacheDir, name)
        var exitsBytes = download.downBytes
        val rangeHeader = "bytes=${download.downBytes}-"
        var lastUpdateTime = System.currentTimeMillis()
        send(Started(name,download))
        client.prepareGet(download.url) {
            header(HttpHeaders.Range, rangeHeader)
            onDownload { _, length ->
                val totalLength = exitsBytes + length
                val progress = (download.downBytes * 100f / totalLength).roundToInt().toString()
                val currentTime = System.currentTimeMillis()
                val timeSinceLastUpdate = currentTime - lastUpdateTime
                if (timeSinceLastUpdate >= 500) {
                    val p = P(progress, download.downBytes.formatSize(), totalLength.formatSize())
                    if (download.downBytes == 0L) send(Waiting(download))
                    else send(Progress(p, download))
                    lastUpdateTime = currentTime
                }
            }
        }.execute {
            if (it.status != HttpStatusCode.PartialContent) {
                file.delete()
                download.downBytes = 0
                exitsBytes = 0
            }
            it.bodyAsChannel().onStreaming(file, download) { s -> send(Speed(s, download)) }
        }
        send(Finished(file, download))
    }.catch { error ->
        error.printStackTrace()
        emit(Failed(error, download))
    }.flowOn(Dispatchers.IO)
}