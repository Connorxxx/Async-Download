package com.connor.asyncdownload.type

import java.io.File

sealed interface DownloadType<out T> {
    data class Waiting<out T>(val m: T) : DownloadType<T>
    data class Started<out T>(val name: String, val m: T) : DownloadType<T>
    data class Progress<out T>(val value: P, val m: T) : DownloadType<T>
    data class Speed<out T>(val value: String, val m: T) : DownloadType<T>
    data class Finished<out T>(val file: File, val m: T) : DownloadType<T>
    data class Failed<out T>(val throwable: Throwable, val m: T) : DownloadType<T>
    data class Pause<out T>(val m: T) : DownloadType<T>
    data class Canceled<out T>(val m: T): DownloadType<T>
}
data class P(
    val p: String,
    val size: String,
    val total: String
)

