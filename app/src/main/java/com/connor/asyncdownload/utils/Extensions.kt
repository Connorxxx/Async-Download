package com.connor.asyncdownload.utils

import com.connor.asyncdownload.model.data.KtorDownload
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import java.io.File
import kotlin.math.log10
import kotlin.math.pow

fun String.getFileNameFromUrl(): String? {
    val regex = ".*/(.+)".toRegex()
    val matchResult = regex.find(this)
    return matchResult?.groupValues?.get(1)
}

fun Long.formatSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    val size = this / 1024.0.pow(digitGroups.toDouble())

    return String.format("%.2f %s", size, units[digitGroups])
}

suspend fun ByteReadChannel.onStreaming(
    file: File,
    download: KtorDownload,
    speed: suspend (String) -> Unit
) {
    var lastUpdateTime = System.currentTimeMillis()
    var lastDownloadedBytes = download.downBytes

    while (!this.isClosedForRead) {
        val buffer = this.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
        while (!buffer.isEmpty) {
            val bytes = buffer.readBytes()
            file.appendBytes(bytes)
            download.downBytes += bytes.size

            val currentTime = System.currentTimeMillis()
            val timeSinceLastUpdate = currentTime - lastUpdateTime

            if (timeSinceLastUpdate >= 500) {
                val downloadedBytesDelta = download.downBytes - lastDownloadedBytes
                val downloadSpeed = downloadedBytesDelta.toDouble() / timeSinceLastUpdate * 1000
                speed(formatSpeed(downloadSpeed))
                lastUpdateTime = currentTime
                lastDownloadedBytes = download.downBytes
            }
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