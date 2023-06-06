package com.connor.asyncdownload.viewmodls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.connor.asyncdownload.model.Repository
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.utils.logCat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    val linkList = ArrayList<Link>()
    var job: Job? = null
        private set

    private val domain = "http://192.168.3.193:8080/"

    fun initLink() {
        linkList.add(Link("https://github.com/MetaCubeX/Clash.Meta/releases/download/v1.14.5/clash.meta-linux-386-cgo-v1.14.5.gz", "clash.apk"))
        linkList.add(Link(domain + "2.apk", "2.apk"))
        linkList.add(Link(domain + "3.apk", "3.apk"))
        linkList.add(Link(domain + "4.apk", "4.apk"))
        linkList.add(Link(domain + "5.apk", "5.apk"))
    }

    fun download(link: Link, block: suspend (DownloadType<Link>) -> Unit) {
        job = viewModelScope.launch {
            link.url.logCat()
            repository.downloadFile(link).collect { block(it) }
        }
    }

}