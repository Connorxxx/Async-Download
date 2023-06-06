package com.connor.asyncdownload.model.data

import java.util.UUID

data class Link(
    val url: String,
    var name: String,
    var progress: String = "0",
    var isEnable: Boolean = true,
    val uuid: String = UUID.randomUUID().toString(),
)
