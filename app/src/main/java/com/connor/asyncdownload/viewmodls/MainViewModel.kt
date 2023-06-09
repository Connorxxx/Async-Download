package com.connor.asyncdownload.viewmodls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connor.asyncdownload.model.Repository
import com.connor.asyncdownload.model.data.DownloadData
import com.connor.asyncdownload.model.data.KtorDownload
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.type.UiState
import com.connor.asyncdownload.utils.logCat
import com.connor.asyncdownload.utils.showToast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.FabClick(false))
    val uiState = _uiState.asStateFlow()

    var fabClick = false

    val loadDownData = repository.loadDownData.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val domain = "http://192.168.3.193:8080/"
    private var i = 1

    //    init {
//        (1..5).forEach {
//            insertDown(DownloadData(KtorDownload("$domain$it.apk")))
//        }
//    }

    fun setUi(uiState: UiState) {
        viewModelScope.launch {
            _uiState.emit(uiState)
        }
    }
    fun addData(list: List<DownloadData>) {
        val url = "$domain$i.apk"
        if (!list.none { it.ktorDownload.url == url }) "Task already exists".showToast()
        else insertDown(DownloadData(KtorDownload(url)))
        i++
    }

    fun insertDown(data: DownloadData) {
        viewModelScope.launch {
            repository.insertDown(data)
        }
    }

    fun updateDowns(data: DownloadData) {
        viewModelScope.launch {
            repository.updateDowns(data)
        }
    }

    fun download(link: DownloadData, block: suspend (DownloadType<KtorDownload>) -> Unit) {
        link.ktorDownload.job = viewModelScope.launch {
            repository.downloadFile(link.ktorDownload).collect { block(it) }
        }
    }
}