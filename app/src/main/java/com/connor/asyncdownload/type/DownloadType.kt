package com.connor.asyncdownload.type

import io.ktor.utils.io.*
import kotlinx.coroutines.Deferred
import java.io.File

sealed interface DownloadType {

    object Started : DownloadType

    data class Progress(
        val value: String,
        val name: String
    ) : DownloadType

    data class Finished(
        val file: File
    ) : DownloadType

    data class Failed(
        val throwable: Throwable
    ) : DownloadType

    data class AsyncDownload(
        val body: Deferred<ByteReadChannel>
    ) : DownloadType
}