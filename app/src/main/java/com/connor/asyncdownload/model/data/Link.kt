package com.connor.asyncdownload.model.data

import java.util.*

data class Link(
    val url: String,
    var name: String,
    var isPause: Boolean = true,
    var state: State = State.Default,
    val uuid: String = UUID.randomUUID().toString(),
)

enum class State{
    Default, Finished, Failed, Downloading
}


