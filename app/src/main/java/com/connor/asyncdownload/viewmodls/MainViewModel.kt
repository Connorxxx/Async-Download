package com.connor.asyncdownload.viewmodls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connor.asyncdownload.model.Repository
import com.connor.asyncdownload.model.data.Animator
import com.connor.asyncdownload.model.data.DownJob
import com.connor.asyncdownload.model.data.DownloadData
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.type.P
import com.connor.asyncdownload.type.UiEvent
import com.connor.asyncdownload.utils.addID
import com.connor.asyncdownload.utils.logCat
import com.connor.asyncdownload.utils.showToast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableSharedFlow<UiEvent<DownloadData>>()
    val uiState = _uiState.asSharedFlow()

    var doAllClick = false

    val jobs = arrayListOf<DownJob>()
    val animas = arrayListOf<Animator>()

    private val domain = "http://192.168.3.193:8080/"
    private var i = 1

    private val loadDownData = repository.loadDownData

    fun loadDownData(block: (List<DownloadData>) -> Unit) {
        viewModelScope.launch {
            loadDownData.collect { block(it) }
        }
    }

    fun setUi(uiState: UiEvent<DownloadData>) {
        viewModelScope.launch { _uiState.emit(uiState) }
    }

    fun addData(list: List<DownloadData>) {
        val url = "$domain$i.apk"
        if (!list.none { it.url == url }) "Task already exists".showToast()
        else insertDown(DownloadData(url))
        i++
    }

    fun insertDown(data: DownloadData) {
        viewModelScope.launch { repository.insertDown(data) }
    }

    fun updateDowns(data: DownloadData) {
        viewModelScope.launch { repository.updateDowns(data) }
    }

    fun download(
        link: DownloadData,
        onDownload: (DownloadData, P) -> Unit,
        onFinish: (DownloadData, File) -> Unit
    ) {
         val job = viewModelScope.launch {
            repository.downloadFile(link).collect {
                _uiState.emit(UiEvent.Download(link, it))
                when (it) {
                    is DownloadType.Started -> "Started vm".logCat()
                    is DownloadType.Progress -> onDownload(it.m, it.value)
                    is DownloadType.Finished -> onFinish(it.m, it.file)
                    else -> {}
                }
            }
        }
        jobs.addID(link.id, DownJob(link.id, job))
    }
}