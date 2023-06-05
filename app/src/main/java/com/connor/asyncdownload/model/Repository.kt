package com.connor.asyncdownload.model

import android.content.Context
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.type.DownloadType
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class Repository @Inject constructor(@ApplicationContext private val ctx: Context, private val client: HttpClient) {

    suspend fun downloadFile(link: Link) = callbackFlow {
        val deferred = async {
            trySend(DownloadType.Started)
            val httpResponse = client.get(link.url) {
                onDownload { bytesSentTotal, contentLength ->
                    val progress = (bytesSentTotal * 100f / contentLength).roundToInt().toString()
                    trySend(DownloadType.Progress(progress, link.name))
                }
            }.bodyAsChannel()
            val file = File(ctx.filesDir, link.name)
            httpResponse.copyAndClose(file.writeChannel())
            trySend(DownloadType.Finished(file))
            httpResponse
        }
        trySend(DownloadType.AsyncDownload(deferred))
        awaitClose { }
    }.catch { error ->
        error.printStackTrace()
        emit(DownloadType.Failed(error))
    }.flowOn(Dispatchers.IO)

}