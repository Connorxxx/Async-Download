package com.connor.asyncdownload.model

import android.content.Context
import com.connor.asyncdownload.model.data.DownloadData
import com.connor.asyncdownload.model.data.KtorDownload
import com.connor.asyncdownload.model.room.DownDao
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.type.P
import com.connor.asyncdownload.utils.formatSize
import com.connor.asyncdownload.utils.getFileNameFromUrl
import com.connor.asyncdownload.utils.logCat
import com.connor.asyncdownload.utils.onStreaming
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class Repository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val client: HttpClient,
    private val downDao: DownDao
) {
    val loadDownData = downDao.loadDown().flowOn(Dispatchers.IO)
    suspend fun insertDown(data: DownloadData) = withContext(Dispatchers.IO) {
        downDao.insertDown(data)
    }

    suspend fun updateDowns(data: DownloadData) = withContext(Dispatchers.IO) {
        downDao.updateDowns(data)
    }

    suspend fun downloadFile(download: KtorDownload) = channelFlow {
        val file = File(ctx.cacheDir, download.url.getFileNameFromUrl() ?: "error")
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