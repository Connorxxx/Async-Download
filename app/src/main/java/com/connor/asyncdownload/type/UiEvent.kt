package com.connor.asyncdownload.type

sealed interface UiEvent<out T> {
    data class DoAllClick(val boolean: Boolean) : UiEvent<Nothing>
    data class Download<T>(val data: T, val type: DownloadType<T>) : UiEvent<T>
    data class StartDownload<T>(val data: T) : UiEvent<T>
}