package com.connor.asyncdownload.type

import java.io.File

sealed interface DownloadType<out T> {

    object Default : DownloadType<Nothing>
    data class Waiting<out T>(val m: T) : DownloadType<T>
    data class Started<out T>(val m: T) : DownloadType<T>
    data class FileExists<out T>(val m: T) : DownloadType<T>
    data class Progress<out T>(val value: String, val m: T) : DownloadType<T>
    data class Speed<out T>(val value: String, val m: T) : DownloadType<T>
    data class Finished<out T>(val file: File, val m: T) : DownloadType<T>
    data class Failed<out T>(val throwable: Throwable, val m: T) : DownloadType<T>
    data class Canceled<out T>(val m: T) : DownloadType<T>
    data class Size<out T>(val size: String, val total: String, val m: T) : DownloadType<T>
}