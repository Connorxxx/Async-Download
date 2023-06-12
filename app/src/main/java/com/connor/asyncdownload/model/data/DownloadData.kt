package com.connor.asyncdownload.model.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "down_data")
data class DownloadData(
    val url: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var downBytes: Long = 0L,
    var fileName: String = "",
    var state: State = State.Default,
    var uriString: String = "",
    @Embedded val uiState: UiState = UiState()
)

data class UiState(
    var p: String = "0",
    var size: String = "",
    var total: String = ""
)

enum class State {
    Default, Finished, Failed, Downloading, Pause, Canceled
}


