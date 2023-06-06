package com.connor.asyncdownload.viewmodls

import androidx.lifecycle.ViewModel
import com.connor.asyncdownload.model.Repository
import com.connor.asyncdownload.model.data.Link
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    val linkList = ArrayList<Link>()

    private val domain = "http://192.168.1.105:8080/"

    fun initLink() {
        linkList.add(Link(domain + "1.apk", "1.apk"))
        linkList.add(Link(domain + "2.apk", "2.apk"))
        linkList.add(Link(domain + "3.apk", "3.apk"))
        linkList.add(Link(domain + "4.apk", "4.apk"))
        linkList.add(Link(domain + "5.apk", "5.apk"))
    }

    suspend fun download(link: Link) = repository.downloadFile(link)

}