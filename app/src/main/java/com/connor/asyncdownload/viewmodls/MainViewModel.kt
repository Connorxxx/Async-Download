package com.connor.asyncdownload.viewmodls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connor.asyncdownload.model.data.Animator
import com.connor.asyncdownload.model.data.DownJob
import com.connor.asyncdownload.model.data.DownloadData
import com.connor.asyncdownload.model.repo.RoomRepository
import com.connor.asyncdownload.type.P
import com.connor.asyncdownload.type.UiEvent
import com.connor.asyncdownload.usecase.DownloadFileUseCase
import com.connor.asyncdownload.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: RoomRepository,
    private val downloadFileUseCase: DownloadFileUseCase,
) : ViewModel() {

    private val _uiState = MutableSharedFlow<UiEvent<DownloadData>>()
    val uiState = _uiState

    var doAllClick = false

    val jobs = arrayListOf<DownJob>()
    val animas = arrayListOf<Animator>()

    private val domain = "http://192.168.10.185:8080/Downloads/temp/"
    private var i = 1

    private val loadDownData = repository.loadDownData

    fun loadDownData(block: (List<DownloadData>) -> Unit) = viewModelScope.launch {
        loadDownData.collect { block(it) }
    }


    fun setUi(uiState: UiEvent<DownloadData>) {
        viewModelScope.launch { _uiState.emit(uiState) }
    }

    fun addData(list: List<DownloadData>) {
        val url = "$domain$i.apk"
        when (list.none { it.url == url }) {
            true -> insertDown(DownloadData(url))
            false -> "Task already exists".showToast()
        }
        i++
    }

    fun insertDown(data: DownloadData) = viewModelScope.launch {
        repository.insertDown(data)
    }


    fun updateDowns(vararg data: DownloadData) = viewModelScope.launch {
        repository.updateDowns(*data)
    }


    fun deleteDowns(vararg data: DownloadData) = viewModelScope.launch {
        repository.deleteDowns(*data)
    }


    fun download(
        link: DownloadData,
        onDownload: (DownloadData, P) -> Unit,
        onFinish: (DownloadData, File) -> Unit
    ) {
        val job = viewModelScope.launch {
            downloadFileUseCase(link) {
                _uiState.emit(UiEvent.Download(link, it))
                when (it) {
                    is Progress -> onDownload(it.m, it.value)
                    is Finished -> onFinish(it.m, it.file)
                    else -> {}
                }
            }
        }
        jobs.addID(link.id, DownJob(link.id, job))
    }
}