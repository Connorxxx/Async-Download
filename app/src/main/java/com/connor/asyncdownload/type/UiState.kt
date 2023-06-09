package com.connor.asyncdownload.type

sealed interface UiState {
    data class FabClick(val boolean: Boolean) : UiState
}