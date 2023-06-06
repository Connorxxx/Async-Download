package com.connor.asyncdownload.utils

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.connor.asyncdownload.App
import com.connor.asyncdownload.BuildConfig
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import java.io.File

inline fun <reified T : ViewBinding> Fragment.viewBinding() =
    ViewBindingDelegate(T::class.java, this)

fun String.showToast() {
    Toast.makeText(App.app, this, Toast.LENGTH_SHORT).show()
}

fun Any.logCat(tab: String = "ASYNC_DOWNLOAD_LOG") {
    if (!BuildConfig.DEBUG) return
    if (this is String) Log.d(tab, this) else Log.d(tab, this.toString())
}

fun String.getFileNameFromUrl(): String? {
    val regex = ".*/(.+)".toRegex()
    val matchResult = regex.find(this)
    return matchResult?.groupValues?.get(1)
}

var downloadedBytes: Long = 0
    private set

suspend fun ByteReadChannel.onStreaming(
    file: File,
    length: Long?,
    speed: suspend (String) -> Unit
) {
    var bytesDownloaded = 0L
    val startTime = System.currentTimeMillis()

    length?.let {
        val skipBytes = downloadedBytes.coerceAtMost(it)
        this.readRemaining(skipBytes)
    }


    while (!this.isClosedForRead) {
        val buffer = this.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
        while (!buffer.isEmpty) {
            val bytes = buffer.readBytes()
            file.appendBytes(bytes)
            bytesDownloaded += bytes.size
            downloadedBytes += bytes.size

            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - startTime
            val downloadSpeed = bytesDownloaded.toDouble() / elapsedTime * 1000
            speed(formatSpeed(downloadSpeed))
        }
    }
}

private fun formatSpeed(speed: Double): String {
    val kilobytes = speed / 1024
    val megabytes = kilobytes / 1024

    return when {
        megabytes >= 1.0 -> String.format("%.2f", megabytes) + "MB/s"
        kilobytes >= 1.0 -> String.format("%.2f", kilobytes) + "KB/s"
        else -> String.format("%.2f", speed) + "B/s"
    }
}