package com.connor.asyncdownload.model.data

data class Link(
    val ktorDownload: KtorDownload,
    var name: String,
    var isPause: Boolean = false,
    var state: State = State.Default,
)

enum class State{
    Default, Finished, Failed, Downloading
}


