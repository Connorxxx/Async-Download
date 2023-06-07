package com.connor.asyncdownload.viewmodls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connor.asyncdownload.model.Repository
import com.connor.asyncdownload.model.data.KtorDownload
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.ui.adapter.DlAdapter
import com.connor.asyncdownload.utils.logCat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    val linkList = ArrayList<Link>()
    private val domain = "http://192.168.3.193:8080/"

    init {
        linkList.add(
            Link(
                KtorDownload("https://github.com/MetaCubeX/Clash.Meta/releases/download/v1.14.5/clash.meta-linux-386-cgo-v1.14.5.gz"),
                0,
                "clash.apk"
            )
        )
        linkList.add(
            Link(
                KtorDownload("https://github.com/Fndroid/clash_for_windows_pkg/releases/download/0.20.24/Clash.for.Windows-0.20.24-win.7z"),
                1,
                "Clash.for.Windows.7z"
            )
        )
        linkList.add(Link(KtorDownload(domain + "3.apk"), 2, "3.apk"))
        linkList.add(Link(KtorDownload(domain + "4.apk"), 3, "4.apk"))
        linkList.add(Link(KtorDownload(domain + "5.apk"), 4, "5.apk"))
    }

    fun download(link: Link, block: suspend (DownloadType<KtorDownload>) -> Unit) {
        link.ktorDownload.job = viewModelScope.launch {
            repository.downloadFile(link.ktorDownload).collect { block(it) }
        }
    }
}