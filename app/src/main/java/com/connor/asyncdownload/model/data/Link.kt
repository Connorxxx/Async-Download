package com.connor.asyncdownload.model.data

data class Link(
    val url: String,
    var name: String,
    var progress: String = "0",
)
