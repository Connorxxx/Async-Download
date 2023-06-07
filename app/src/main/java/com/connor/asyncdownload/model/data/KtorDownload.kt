package com.connor.asyncdownload.model.data

import kotlinx.coroutines.Job
import java.util.*

data class KtorDownload(
    val url: String,
    val uuid: String = UUID.randomUUID().toString(),
    var downBytes: Long = 0L,
    var job: Job? = null,
)
