package com.connor.asyncdownload.type

sealed interface Event

data class Id(val id: Int) : Event

