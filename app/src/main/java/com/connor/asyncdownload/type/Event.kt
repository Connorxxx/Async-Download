package com.connor.asyncdownload.type

sealed interface Event

data class Cancel(val id: Int) : Event

