package com.connor.asyncdownload.model

import android.content.Context
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.utils.logCat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
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
                send(DownloadType.Progress(progress,link.name))
            }
        }.bodyAsChannel()
        val file = File(ctx.filesDir, link.name)
        httpResponse.copyAndClose(file.writeChannel())
        send(DownloadType.Finished(file))
    }.onStart {
        emit(DownloadType.Started)
    }.catch { error ->
        error.printStackTrace()
        emit(DownloadType.Failed(error))
    }.flowOn(Dispatchers.IO)

}