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
       // linkList.add(Link("http://192.168.1.105:8080/1.apk", "1.apk"))
        linkList.add(Link("http://192.168.1.105:8080/2.apk", "2.apk"))
       // linkList.add(Link("http://192.168.1.105:8080/3.apk", "3.apk"))
       // linkList.add(Link("http://192.168.1.105:8080/4.apk", "4.apk"))
        linkList.add(Link("http://192.168.1.105:8080/5.apk", "5.apk"))
    }

    suspend fun download(link: Link) = repository.downloadFile(link)

}