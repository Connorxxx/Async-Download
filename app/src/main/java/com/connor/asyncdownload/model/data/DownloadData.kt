package com.connor.asyncdownload.model.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "down_data")
data class DownloadData(
    @Embedded val ktorDownload: KtorDownload,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var fileName: String = "",
    var state: State = State.Default,
    var uriString: String = "",
    @Embedded val uiState: UiState = UiState()
)

data class KtorDownload(
    val url: String,
    var downBytes: Long = 0L
)

data class UiState(
    var p: String = "0",
    var size: String = "",
    var total: String = ""
)

enum class State {
    Default, Finished, Failed, Downloading, Pause, Canceled
}


