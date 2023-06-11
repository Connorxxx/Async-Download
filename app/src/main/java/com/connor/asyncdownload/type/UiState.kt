package com.connor.asyncdownload.type

import com.connor.asyncdownload.model.data.DownloadData

sealed interface UiState<out T> {
    data class FabClick(val boolean: Boolean) : UiState<Nothing>

    data class Download<T>(val link: DownloadData, val type: DownloadType<T>) : UiState<T>
}