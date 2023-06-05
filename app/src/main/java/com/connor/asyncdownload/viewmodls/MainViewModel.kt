package com.connor.asyncdownload.viewmodls

import androidx.lifecycle.ViewModel
import com.connor.asyncdownload.model.Repository
import com.connor.asyncdownload.model.data.Link
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    val linkList = ArrayList<Link>()

    fun initLink() {
        linkList.add(Link("http://192.168.10.186:8080/Downloads/temp/1.flac", "1.flac"))
        linkList.add(Link("http://192.168.10.186:8080/Downloads/temp/2.flac", "2.flac"))
        linkList.add(Link("http://192.168.10.186:8080/Downloads/temp/3.flac", "3.flac"))
        linkList.add(Link("http://192.168.10.186:8080/Downloads/temp/4.flac", "4.flac"))
        linkList.add(Link("http://192.168.10.186:8080/Downloads/temp/5.flac", "5.flac"))
    }

    suspend fun download(link: Link) = repository.downloadFile(link)

}