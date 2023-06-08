package com.connor.asyncdownload.viewmodls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connor.asyncdownload.model.Repository
import com.connor.asyncdownload.model.data.DownloadData
import com.connor.asyncdownload.model.data.KtorDownload
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.utils.logCat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val domain = "http://192.168.3.193:8080/"

    var fabState = false

    val loadDownData = repository.loadDownData.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

//    init {
//        (1..5).forEach {
//            insertDown(DownloadData(KtorDownload("$domain$it.apk")))
//        }
//    }
    fun insertDown(data: DownloadData) {
        viewModelScope.launch {
            repository.insertDown(data)
        }
    }

    fun download(link: DownloadData, block: suspend (DownloadType<KtorDownload>) -> Unit) {
        link.ktorDownload.job = viewModelScope.launch {
            repository.downloadFile(link.ktorDownload).collect { block(it) }
        }
    }
}