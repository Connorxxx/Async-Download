package com.connor.asyncdownload.type

import java.io.File

sealed interface DownloadType<out T> {
    object Waiting : DownloadType<Nothing>
    data class Started<out T>(val m: T) : DownloadType<T>
    data class Progress<out T>(val value: String, val m: T) : DownloadType<T>
    data class Finished<out T>(val file: File, val m: T) : DownloadType<T>
    data class Failed<out T>(val throwable: Throwable, val m: T) : DownloadType<T>
}